package lavs.service;

import lavs.domain.LoanSummary;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;
import lavs.gateway.LoanGateway;
import lavs.gateway.OperationResult;

// AI-assisted: validation checks and gateway delegation structure outlined by AI; code written by me.
/**
 * Retrieves mainframe-calculated repayment summaries and schedule pages.
 * Rejects missing loan identifiers and invalid pagination locally with error
 * code 3 before invoking the gateway. Gateway calls are synchronous and should
 * run on the shared background worker.
 */
public class RepaymentServiceImpl implements RepaymentService {

    private final LoanGateway loanGateway;

    public RepaymentServiceImpl(LoanGateway loanGateway) {
        this.loanGateway = loanGateway;
    }

    @Override
    public OperationResult<LoanSummary> getSummary(String loanNumber) {
        if (loanNumber == null || loanNumber.isBlank()) {
            return OperationResult.failure(3, "Loan number is required", 0);
        }

        return loanGateway.getSummary(loanNumber);
    }

    @Override
    public OperationResult<Page<RepaymentScheduleItem>> getSchedule(String loanNumber, long page, long size) {
        if (loanNumber == null || loanNumber.isBlank()) {
            return OperationResult.failure(3, "Loan number is required", 0);
        }

        if (page < 1) {
            return OperationResult.failure(3, "Page must be at least 1", 0);
        }

        if (size < 1) {
            return OperationResult.failure(3, "Size must be at least 1", 0);
        }

        return loanGateway.getRepaymentSchedule(loanNumber, page, size);
    }

}
