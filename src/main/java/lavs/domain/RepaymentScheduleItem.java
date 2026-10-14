package lavs.domain;

import java.time.LocalDate;

public record RepaymentScheduleItem(
    long paymentNumber,
    LocalDate date,
    double amountPaid,
    double principalAmount,
    double interestAmount,
    double amountRemaining
) {}
