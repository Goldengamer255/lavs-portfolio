package lavs.domain;

import java.time.LocalDate;

/**
 * Saved loan terms and status associated with a borrowing customer.
 * Interest-rate frequency and payment frequency are separate terms; optional
 * dates are represented by null when absent from a mainframe response.
 */
public record Loan(
    String loanNumber,
    String customerNumber, 
    double principal,
    double paymentAmount,
    double rateAmount,
    String rateType,
    String rateFrequency,
    String paymentFrequency,
    LocalDate startDate,
    LocalDate renewalDate,
    String status
) {}