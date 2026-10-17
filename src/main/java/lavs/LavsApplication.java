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
import lavs.service.DocumentService;
import lavs.service.LoanService;
import lavs.service.NoteService;
import lavs.service.RepaymentService;
import lavs.service.RepaymentServiceImpl;
import lavs.validation.CustomerRules;
import lavs.validation.LoanRules;
import lavs.validation.ValidationService;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;

public class LavsApplication extends Application {

    private ApplicationShell shell;
    private Connection connection;

    @Override
    public void start(Stage stage) {
        AppConfig config = AppConfig.load();
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
                connectionMonitor,
                worker);
        shell.start();
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (shell != null) {
            shell.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
