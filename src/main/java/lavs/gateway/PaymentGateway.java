package lavs.gateway;

import lavs.domain.Page;
import lavs.domain.Payment;
import lavs.domain.PaymentSummary;

import java.math.BigDecimal;
import java.time.LocalDate;

// Mainframe access for loan payments, kept separate from LoanGateway.
public interface PaymentGateway {
    OperationResult<Page<PaymentSummary>> listPayments(String loanNumber, long page, long size);

    OperationResult<Payment> retrievePayment(String loanNumber, long id);

    OperationResult<Payment> addPayment(String loanNumber, BigDecimal amountPaid, LocalDate date);

    OperationResult<Boolean> deletePayment(String loanNumber, long id);
}
