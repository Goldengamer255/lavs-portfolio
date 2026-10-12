package lavs.ui;

import java.util.function.Consumer;

/**
 * Screen navigation contract for LAVS controllers.
 */
public interface Navigator {

    void showLogin();

    void showCustomerSearch();

    void showNewCustomer();

    void showCustomerDetails(String customerNumber);

    void showNewLoan(String customerNumber);

    void showLoanDetails(String loanNumber);

    void showLoanSummary(String loanNumber);

    /**
     * Opens customer selection for the specified loan.
     * On selection, restores the originating loan screen and passes
     * the selected customer number to onSelected.
     * Cancelling returns without calling onSelected.
     * Navigation itself does not link the customer to the loan.
     */
    void pickCoBorrower(String loanNumber, Consumer<String> onSelected);

    /**
     * Returns to the previous screen, preserving its context.
     * Does nothing if there is no previous screen.
     */
    void back();
}