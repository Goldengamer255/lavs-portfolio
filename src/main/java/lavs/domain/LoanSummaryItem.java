package lavs.domain;

import java.time.LocalDate;

/**
 * Compact loan entry used when listing a customer's loans.
 * Contains identifying terms and status; repayment totals are supplied separately
 * by {@link LoanSummary}.
 */
public record LoanSummaryItem(
    String loanNumber, 
    double principal, 
    LocalDate startDate, 
    String status
) {}
