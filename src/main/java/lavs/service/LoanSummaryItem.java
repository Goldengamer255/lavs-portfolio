package lavs.service;

/** Service response data, independent of mainframe message classes. */
// AI-assisted: service contract scaffolding for team review.
public record LoanSummaryItem(String loanNumber, String principal, String startDate, String status) {}
