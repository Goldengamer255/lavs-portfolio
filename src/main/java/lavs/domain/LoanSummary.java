package lavs.domain;

import java.time.LocalDate;
//shows a summary of a single loan, not to be confused with LoanListItem which shows a customers loans
public record LoanSummary(
    String loanNumber,
    String customerName,
    double totalAmountPaid,
    double totalInterestPaid,
    long numberOfPayments,
    LocalDate finalPaymentDate,
    double finalPaymentAmount
) {}
