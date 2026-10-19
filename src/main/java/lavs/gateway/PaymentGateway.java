package lavs.gateway;

import lavs.domain.Page;
import lavs.domain.Payment;
import lavs.domain.PaymentSummary;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Provides access to recorded loan payments and their mainframe-calculated details.
 * Payment amounts use {@link BigDecimal} to preserve decimal values.
 */
public interface PaymentGateway {
    /** Lists payment summaries using a one-based page number and a positive page size. */
    OperationResult<Page<PaymentSummary>> listPayments(String loanNumber, long page, long size);

    /** Retrieves a payment with its principal, interest, and remaining balance details. */
    OperationResult<Payment> retrievePayment(String loanNumber, long id);

    OperationResult<Payment> addPayment(String loanNumber, BigDecimal amountPaid, LocalDate date);

    /** Deletes the identified payment and acknowledges success with true. */
    OperationResult<Boolean> deletePayment(String loanNumber, long id);
}
