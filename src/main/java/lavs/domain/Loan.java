package lavs.domain;

public record Loan(
    String loanNumber,
    String customerNumber, 
    double principal,
    double paymentAmount,
    double rateAmount,
    String rateType,
    String rateFrequency,
    String paymentFrequency,
    String startDate,
    String renewalDate,
    String status
) {}