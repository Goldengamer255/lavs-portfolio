package lavs.ui;

import java.util.function.Consumer;

/**
 * Screen navigation contract for LAVS controllers.
 * After showing a screen and supplying its navigation context, implementations
 * call {@link ScreenLifecycle#onShow()} if its controller implements that interface.
 */
public interface Navigator {

    /**
     * Opens LoginView and clears navigation history and any active co-borrower
     * selection, without invoking its callback. Back cannot restore a previous
     * authenticated screen. The authentication service handles logout itself.
     */
    void showLogin();

    void showCustomerSearch();

    /**
     * Opens CustomerDetailsView with blank, editable fields in creation mode.
     * Preserves an active co-borrower selection flow.
     */
    void showNewCustomer();

    /** Opens CustomerDetailsView for the customer, preserving any active pick mode. */
    void showCustomerDetails(String customerNumber);

    /**
     * Opens LoanDetailsView with blank, editable loan fields in creation mode,
     * with the specified customer as the applicant.
     */
    void showNewLoan(String customerNumber);

    void showLoanDetails(String loanNumber);

    void showLoanSummary(String loanNumber);

    /**
     * Opens CustomerSearchView in selection mode for the specified loan.
     * Pick mode remains active while opening or creating a customer, allowing
     * CustomerDetailsView to offer a "Link as co-borrower" action.
     * Completion uses completeCoBorrowerPick; cancellation uses back.
     * Navigation itself does not link the customer to the loan.
     * Only one selection flow may be active at a time.
     */
    void pickCoBorrower(String loanNumber, Consumer<String> onSelected);

    /** Returns whether a co-borrower selection flow is currently active. */
    boolean isPickingCoBorrower();

    /**
     * Ends pick mode, removes its temporary navigation history, and restores
     * the originating loan screen with its context intact. Then invokes the
     * selection callback exactly once with the selected customer number.
     * The callback's recipient handles linking through the loan service.
     *
     * @throws IllegalStateException if no selection flow is active
     */
    void completeCoBorrowerPick(String customerNumber);

    /**
     * Returns to the previous screen, preserving its context.
     * During co-borrower selection, cancels the entire selection flow instead,
     * clears pick mode and its temporary history, and restores the originating
     * loan screen without invoking the selection callback.
     * Restored controllers receive onShow so they can refresh service data
     * while retaining unsaved input and local screen state.
     * Does nothing if there is no previous screen.
     */
    void back();
}
