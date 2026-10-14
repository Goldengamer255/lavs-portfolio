package lavs.ui;

/** Loan details/summary controllers receive context before their first onShow call. */
public interface LoanContextAware {
    void setLoanContext(LoanScreenContext context);
}
