package lavs.domain;

import java.time.LocalDate;

public record LoanSummaryItem(
    String loanNumber, 
    double principal, 
    LocalDate startDate, 
    String status
) {}
