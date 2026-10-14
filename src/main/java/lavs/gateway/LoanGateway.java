package lavs.gateway;

import lavs.domain.Coborrower;
import lavs.domain.Loan;
import lavs.domain.LoanListItem;
import lavs.domain.LoanSummary;
import lavs.domain.NewLoan;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;

public interface LoanGateway {
    OperationResult<Loan> create(NewLoan input);

    OperationResult<Loan> retrieve(String loanNumber);

    OperationResult<String> changeStatus(String loanNumber, String newStatus);

    OperationResult<LoanSummary> getSummary(String loanNumber);

    OperationResult<Loan> update(Loan loan);

    OperationResult<Page<RepaymentScheduleItem>> getRepaymentSchedule(String loanNumber, long page, long size);

    // LoanListItem shows a customers loans, not to be confused with LoanSummary which shows a summary of a single loan
    OperationResult<Page<LoanListItem>> listLoans(String customerNumber, long page, long size);

    // Coborrower related methods
    OperationResult<Long> addCoborrower(String loanNumber, String coborrowerNumber, String type);

    OperationResult<Boolean> deleteCoborrower(String loanNumber, long id);

    OperationResult<Page<Coborrower>> listCoborrowers(String loanNumber, long page, long size);

    OperationResult<Coborrower> retrieveCoborrower(String loanNumber, long id);
}
