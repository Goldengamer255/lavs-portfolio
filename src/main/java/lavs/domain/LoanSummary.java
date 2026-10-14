package lavs.domain;

import java.time.LocalDate;
// Summary data for a single loan.
public record LoanSummary(
    String loanNumber,
    String customerName,
    double totalAmountPaid,
    double totalInterestPaid,
    long numberOfPayments,
    LocalDate finalPaymentDate,
    double finalPaymentAmount
) {}
