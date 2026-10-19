package lavs.domain;

import java.time.LocalDate;

/**
 * One entry in a loan's mainframe-provided repayment schedule.
 * The payment number identifies its sequence position; amounts include the
 * principal and interest components and the remaining balance.
 */
public record RepaymentScheduleItem(
    long paymentNumber,
    LocalDate date,
    double amountPaid,
    double principalAmount,
    double interestAmount,
    double amountRemaining
) {}
