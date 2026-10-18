package lavs;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javafx.application.Application;
import javafx.stage.Stage;
import lavs.adapter.*;
import lavs.gateway.CustomerGateway;
import lavs.gateway.LoanGateway;
import lavs.service.*;
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
        CustomerGateway customerGateway = new CustomerAdapter(connection);
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
