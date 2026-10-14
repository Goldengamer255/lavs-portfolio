package lavs.domain;

import java.time.LocalDate;

public record LoanSummary(
    String loanNumber,
    String customerName,
    double totalAmountPaid,
    double totalInterestPaid,
    long numberOfPayments,
    LocalDate finalPaymentDate,
    double finalPaymentAmount
) {}
