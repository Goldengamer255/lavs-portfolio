package lavs.domain;

public record NewLoan(
    String customerNumber,
    double principal,
    double paymentAmount,
    double rateAmount,
    String rateType,
    String rateFrequency,
    String paymentFrequency,
    String startDate,
    String renewalDate
) {}