package lavs;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javafx.application.Application;
import javafx.stage.Stage;
import lavs.adapter.CustomerAdapter;
import lavs.adapter.LoanAdapter;
import lavs.adapter.MainframeAuthenticationGateway;
import lavs.adapter.MainframeCategoryGateway;
import lavs.adapter.NoteAdapter;
import lavs.adapter.PaymentAdapter;
import lavs.adapter.RecoveringConnection;
import lavs.gateway.LoanGateway;
import lavs.service.AuthenticationService;
import lavs.service.CategoryService;
import lavs.service.CustomerService;
import lavs.service.DefaultAuthenticationService;
import lavs.service.DefaultCategoryService;
import lavs.service.DefaultConnectionMonitor;
import lavs.service.DefaultCustomerService;
import lavs.service.DefaultLoanService;
import lavs.service.DefaultNoteService;
import lavs.service.DefaultPaymentService;
import lavs.service.DocumentService;
import lavs.service.LoanService;
import lavs.service.NoteService;
import lavs.service.PaymentService;
import lavs.service.RepaymentService;
import lavs.service.RepaymentServiceImpl;
import lavs.validation.CustomerRules;
import lavs.validation.LoanRules;
import lavs.validation.ValidationService;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;

/**
 * JavaFX entry point for the Loan Application and Verification System.
 *
 * <p>Assembles the mainframe gateways, business services, and application shell.
 * The shell manages the shared worker and connection monitor; this application
 * retains responsibility for closing the mainframe connection.</p>
 */
public class LavsApplication extends Application {

    private ApplicationShell shell;
    private Connection connection;

    /**
     * Builds application-wide dependencies and displays the initial login view.
     * Called by JavaFX on the application thread.
     *
     * @param stage primary window supplied by the JavaFX runtime
     */
    @Override
    public void start(Stage stage) {
        AppConfig config = AppConfig.load();
        // Share one recoverable session across gateways so authentication and
        // subsequent mainframe requests use the same connection.
        connection = new RecoveringConnection(new HttpConnection(config.baseUrl()));
        AuthenticationService authentication = new DefaultAuthenticationService(
                new MainframeAuthenticationGateway(connection));
        CategoryService categories = new DefaultCategoryService(
                new MainframeCategoryGateway(connection));
        NoteService notes = new DefaultNoteService(new NoteAdapter(connection));
        CustomerAdapter customerGateway = new CustomerAdapter(connection);
        DocumentService documents = new DocumentService(customerGateway);
        LoanGateway loanGateway = new LoanAdapter(connection);
        ValidationService validation = new ValidationService();
        CustomerService customers = new DefaultCustomerService(
                customerGateway,
                loanGateway,
                validation,
                new CustomerRules(),
                config.organisationId());
        LoanService loans = new DefaultLoanService(
                loanGateway,
                validation,
                new LoanRules());
        RepaymentService repayments = new RepaymentServiceImpl(loanGateway);
        PaymentService payments = new DefaultPaymentService(new PaymentAdapter(connection), validation);
        // Serialize service calls and connection probes off the JavaFX thread.
        // The shell shuts down this worker; daemon status also prevents it from
        // keeping the JVM alive after the application exits.
        ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "lavs-mainframe-worker");
            thread.setDaemon(true);
            return thread;
        });
        DefaultConnectionMonitor connectionMonitor =
                new DefaultConnectionMonitor(connection, worker);

        stage.setTitle("Countrywide — Loan Application and Verification System");
        shell = new ApplicationShell(
                stage,
                authentication,
                customers,
                documents,
                loans,
                categories,
                notes,
                repayments,
                payments,
                connectionMonitor,
                worker);
        shell.start();
        stage.show();
    }

    /**
     * Releases shell resources, then closes the shared mainframe connection.
     *
     * @throws Exception if resource cleanup fails
     */
    @Override
    public void stop() throws Exception {
        // Cancel monitoring and request worker shutdown before closing the
        // connection used by background tasks.
        if (shell != null) {
            shell.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Launches the JavaFX runtime, which manages the application lifecycle.
     *
     * @param args command-line arguments passed to JavaFX
     */
    public static void main(String[] args) {
        launch(args);
    }
}
