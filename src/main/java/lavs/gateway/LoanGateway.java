package lavs.gateway;

import lavs.domain.Loan;
import lavs.domain.NewLoan;

public interface LoanGateway {
    OperationResult<Loan> create(NewLoan input);

    OperationResult<Loan> retrieve(String loanNumber);

    OperationResult<Loan> changeStatus(String loanNumber, String newStatus);
}
