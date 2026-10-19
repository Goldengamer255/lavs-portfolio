package lavs;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.stage.Stage;
import lavs.service.AuthenticationService;
import lavs.service.CategoryService;
import lavs.service.ConnectionMonitor;
import lavs.service.CustomerService;
import lavs.service.DocumentService;
import lavs.service.LoanService;
import lavs.service.NoteService;
import lavs.service.PaymentService;
import lavs.service.RepaymentService;
import lavs.ui.ControllerFactory;
import lavs.ui.FxmlNavigator;
import lavs.ui.ShellDependencies;

/**
 * Wires navigation and controllers to the application's shared services.
 *
 * <p>Maintains the observable connection state used by the UI and owns cleanup
 * of the connection subscription, monitor, and background worker. The underlying
 * mainframe connection is closed separately by {@link LavsApplication}.</p>
 */
// AI-assisted: application composition and shutdown structure.
public final class ApplicationShell implements AutoCloseable {

    private final ScheduledExecutorService worker;
    private final ConnectionMonitor connectionMonitor;
    private final ConnectionMonitor.Subscription connectionSubscription;
    private final FxmlNavigator navigator;
    private final BooleanProperty online = new SimpleBooleanProperty(false);
    private final ShellDependencies dependencies;

    /**
     * Creates the navigation and controller infrastructure using shared services.
     *
     * <p>The supplied monitor and worker are shut down by {@link #close()}.
     * Construction registers the connection listener; {@link #start()} begins
     * monitoring and displays the login view.</p>
     */
    public ApplicationShell(Stage stage, AuthenticationService authentication,
            CustomerService customers, DocumentService documents, LoanService loans, CategoryService categories,
            NoteService notes, RepaymentService repayments, PaymentService payments,
            ConnectionMonitor connectionMonitor, ScheduledExecutorService worker) {
        Objects.requireNonNull(stage, "stage");
        this.connectionMonitor = Objects.requireNonNull(connectionMonitor, "connectionMonitor");
        this.worker = Objects.requireNonNull(worker, "worker");

        navigator = new FxmlNavigator(stage, authentication, online, worker);
        ControllerFactory controllers = new ControllerFactory(
                authentication, customers, documents, loans, categories, notes, repayments, payments,
                navigator, worker);
        // Install dependency-aware controller creation before loading any views.
        navigator.setControllerFactory(controllers);

        dependencies = new ShellDependencies(navigator, authentication, customers, documents, loans,
                categories, notes, repayments, payments, connectionMonitor, worker, online);
        connectionSubscription = connectionMonitor.subscribe(this::updateOnline);
    }

    /**
     * Starts connection monitoring and opens the login view.
     * Must be called on the JavaFX application thread.
     */
    public void start() {
        connectionMonitor.start();
        navigator.showLogin();
    }

    /**
     * Returns the shared dependencies and observable connection state.
     *
     * @return dependency bundle assembled for this shell
     */
    public ShellDependencies dependencies() {
        return dependencies;
    }

    private void updateOnline(boolean connected) {
        // Monitor notifications may arrive from the background worker; changes
        // to UI-bound state must be applied on the JavaFX application thread.
        if (Platform.isFxApplicationThread()) {
            online.set(connected);
        } else {
            Platform.runLater(() -> online.set(connected));
        }
    }

    /**
     * Detaches the connection listener, stops monitoring, and requests immediate
     * worker shutdown. Running tasks are interrupted, but this method does not
     * wait for them to finish.
     */
    @Override
    public void close() {
        connectionSubscription.close();
        connectionMonitor.close();
        worker.shutdownNow();
    }
}
