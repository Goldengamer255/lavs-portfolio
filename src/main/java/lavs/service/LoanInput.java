package lavs.service;

/** Raw form values; parsing and business validation belong in the service layer. */
// AI-assisted: service contract scaffolding for team review.
public record LoanInput(String principal, String paymentAmount, String rateAmount, String rateType, String rateFrequency, String paymentFrequency, java.time.LocalDate startDate, java.time.LocalDate renewalDate) {}
