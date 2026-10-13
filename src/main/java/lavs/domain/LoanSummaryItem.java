package lavs.domain;

import java.time.LocalDate;

/** Service response data, independent of mainframe message classes. */
// AI-assisted: service contract scaffolding for team review.
public record LoanSummaryItem(String loanNumber, double principal, LocalDate startDate, String status) {}
