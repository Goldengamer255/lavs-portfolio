package lavs.domain;

import java.time.LocalDate;

    // LoanListItem shows a customers loans, not to be confused with LoanSummary which shows a summary of a single loan
public record LoanListItem(
    String loanNumber,
    double principal,
    LocalDate startDate,
    String status
) {}
