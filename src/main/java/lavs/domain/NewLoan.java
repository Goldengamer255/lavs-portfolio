package lavs.domain;

import java.time.LocalDate;

/**
 * Input for creating a loan for an existing customer.
 * Contains the proposed loan terms; the assigned loan number and status are
 * returned in the resulting {@link Loan}.
 */
public record NewLoan(
    String customerNumber,
    double principal,
    double paymentAmount,
    double rateAmount,
    String rateType,
    String rateFrequency,
    String paymentFrequency,
    LocalDate startDate,
    LocalDate renewalDate
) {}