package lavs.service;

import lavs.domain.LoanSummary;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;
import lavs.gateway.OperationResult;

/**
 * This is the Loan summary and repayment schedule. The mainframe calculates both, therefore
 * implementations only retrieve and map them. Page numbers are one-based and
 * sizes are positive and network operations must run on the shared worker.
 */

// AI-assisted: RepaymentService interface constructors.
public interface RepaymentService {
    OperationResult<LoanSummary> getSummary(String loanNumber);

    OperationResult<Page<RepaymentScheduleItem>> getSchedule(String loanNumber, long page, long size);
}
