package lavs.gateway;

import lavs.domain.Loan;
import lavs.domain.LoanSummary;
import lavs.domain.NewLoan;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;

public interface LoanGateway {
    OperationResult<Loan> create(NewLoan input);

    OperationResult<Loan> retrieve(String loanNumber);

    OperationResult<String> changeStatus(String loanNumber, String newStatus);

    OperationResult<LoanSummary> getSummary(String loanNumber);

    OperationResult<Page<RepaymentScheduleItem>> getRepaymentSchedule(String loanNumber, long page, long size);

}
