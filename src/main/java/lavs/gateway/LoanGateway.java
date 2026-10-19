package lavs.gateway;

import lavs.domain.CoBorrower;
import lavs.domain.Loan;
import lavs.domain.LoanSummaryItem;
import lavs.domain.LoanSummary;
import lavs.domain.NewLoan;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;

/**
 * Defines access to loan terms, repayment information, and co-borrower associations.
 *
 * <p>Paged operations accept a one-based page number and a positive page size.
 * Returned pages include the total item count for navigation. Callers inspect
 * {@link OperationResult} before consuming response values.</p>
 */
public interface LoanGateway {
    /** Creates a loan and returns its assigned number and saved terms. */
    OperationResult<Loan> create(NewLoan input);

    OperationResult<Loan> retrieve(String loanNumber);

    OperationResult<String> changeStatus(String loanNumber, String newStatus);

    /** Retrieves repayment totals calculated by the mainframe. */
    OperationResult<LoanSummary> getSummary(String loanNumber);

    OperationResult<Loan> update(Loan loan);

    OperationResult<Page<RepaymentScheduleItem>> getRepaymentSchedule(String loanNumber, long page, long size);

    OperationResult<Page<LoanSummaryItem>> listLoans(String customerNumber, long page, long size);

    // Co-borrower identifiers refer to associations, not customer records.
    /** Links a customer to the loan and returns the new association identifier. */
    OperationResult<Long> addCoborrower(String loanNumber, String coborrowerNumber, String type);

    /** Removes the association identified by id and acknowledges success with true. */
    OperationResult<Boolean> deleteCoborrower(String loanNumber, long id);

    OperationResult<Page<CoBorrower>> listCoborrowers(String loanNumber, long page, long size);

    OperationResult<CoBorrower> retrieveCoborrower(String loanNumber, long id);
}
