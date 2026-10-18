package lavs.ui;

import java.util.concurrent.ExecutorService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import lavs.service.*;

/**
 * Dependencies owned by the application shell. The controller factory passes
 * required members into controller constructors. worker is a single shared
 * serial executor, also used by TaskRunner and connection probes. The shell
 * owns shutdown; controllers must not shut it down. online is updated on the
 * JavaFX thread and is read-only to controllers, which can bind to online.not().
 */
// AI-assisted: controller injection contract for team review.
public record ShellDependencies(
        Navigator navigator,
        AuthenticationService authentication,
        CustomerService customers,
        LoanService loans,
        CategoryService categories,
        NoteService notes,
        RepaymentService repayments,
        PaymentService payments,
        ConnectionMonitor connectionMonitor,
        ExecutorService worker,
        ReadOnlyBooleanProperty online) {}
