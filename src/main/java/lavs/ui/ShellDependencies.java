package lavs.ui;

import java.util.concurrent.ExecutorService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import lavs.service.AuthenticationService;
import lavs.service.CategoryService;
import lavs.service.ConnectionMonitor;
import lavs.service.CustomerService;
import lavs.service.DocumentService;
import lavs.service.LoanService;
import lavs.service.NoteService;
import lavs.service.RepaymentService;

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
        DocumentService documents,
        LoanService loans,
        CategoryService categories,
        NoteService notes,
        RepaymentService repayments,
        ConnectionMonitor connectionMonitor,
        ExecutorService worker,
        ReadOnlyBooleanProperty online) {}
