package lavs.domain;

import java.time.LocalDate;

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