package lavs.domain;

/** Service response data, independent of mainframe message classes. */
// AI-assisted: service contract scaffolding for team review.
public record CoBorrower(long id, String loanNumber, String customerNumber, String customerName, String type) {}
