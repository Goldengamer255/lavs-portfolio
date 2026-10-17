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
import lavs.service.LoanService;
import lavs.service.NoteService;
import lavs.service.RepaymentService;
import lavs.ui.ControllerFactory;
import lavs.ui.FxmlNavigator;
import lavs.ui.ShellDependencies;

/** Owns application-wide dependencies and their lifecycle. */
// AI-assisted: application composition and shutdown structure.
public final class ApplicationShell implements AutoCloseable {

    private final ScheduledExecutorService worker;
    private final ConnectionMonitor connectionMonitor;
    private final ConnectionMonitor.Subscription connectionSubscription;
    private final FxmlNavigator navigator;
    private final BooleanProperty online = new SimpleBooleanProperty(false);
    private final ShellDependencies dependencies;

    public ApplicationShell(Stage stage, AuthenticationService authentication,
            CustomerService customers, LoanService loans, CategoryService categories,
            NoteService notes, RepaymentService repayments,
            ConnectionMonitor connectionMonitor, ScheduledExecutorService worker) {
        Objects.requireNonNull(stage, "stage");
        this.connectionMonitor = Objects.requireNonNull(connectionMonitor, "connectionMonitor");
        this.worker = Objects.requireNonNull(worker, "worker");

        navigator = new FxmlNavigator(stage, authentication, online, worker);
        ControllerFactory controllers = new ControllerFactory(
                authentication, customers, loans, categories, notes, repayments,
                navigator, worker);
        navigator.setControllerFactory(controllers);

        dependencies = new ShellDependencies(navigator, authentication, customers, loans,
                categories, notes, repayments, connectionMonitor, worker, online);
        connectionSubscription = connectionMonitor.subscribe(this::updateOnline);
    }

    public void start() {
        connectionMonitor.start();
        navigator.showLogin();
    }

    public ShellDependencies dependencies() {
        return dependencies;
    }

    private void updateOnline(boolean connected) {
        if (Platform.isFxApplicationThread()) {
            online.set(connected);
        } else {
            Platform.runLater(() -> online.set(connected));
        }
    }

    @Override
    public void close() {
        connectionSubscription.close();
        connectionMonitor.close();
        worker.shutdownNow();
    }
}
