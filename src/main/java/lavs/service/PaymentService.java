package lavs.service;

import lavs.domain.Page;
import lavs.domain.Payment;
import lavs.domain.PaymentSummary;
import lavs.gateway.OperationResult;

import java.util.List;

/** Loan payments. Writes validate first; there is no update because the mainframe has none. */
public interface PaymentService {

    OperationResult<Page<PaymentSummary>> listPayments(String loanNumber, long page, long size);

    OperationResult<List<PaymentSummary>> listAllPayments(String loanNumber);

    OperationResult<Payment> getPayment(String loanNumber, long id);

    OperationResult<SaveResult<Payment>> addPayment(String loanNumber, PaymentInput input);

    OperationResult<Boolean> deletePayment(String loanNumber, long id);

    ValidationReport validatePayment(PaymentInput input);
}
