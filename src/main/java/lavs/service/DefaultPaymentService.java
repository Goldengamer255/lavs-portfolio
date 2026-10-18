package lavs.service;

import lavs.domain.Page;
import lavs.domain.Payment;
import lavs.domain.PaymentSummary;
import lavs.gateway.OperationResult;
import lavs.gateway.PaymentGateway;
import lavs.validation.ValidationService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// AI-assisted: structure and the invalid/saved helpers adapted from DefaultNoteService with help from AI; service methods written by me
/**
 * Validates payment input before it reaches the mainframe, then records the payment
 * through the gateway.
 *
 * <p>An invalid payment is returned as a successful result that carries the validation
 * issues, and nothing is sent to the mainframe. Mainframe and network failures are passed
 * back as failure results. The mainframe has no update message for payments, so payments
 * can only be listed, viewed, added or deleted.
 */
public class DefaultPaymentService implements PaymentService {
    static final int PAGE_SIZE = 50;
    private final PaymentGateway gateway;
    private final ValidationService validation;

    public DefaultPaymentService(PaymentGateway gateway, ValidationService validation) {
        this.gateway = gateway;
        this.validation = validation;
    }

    @Override
    public OperationResult<Page<PaymentSummary>> listPayments(String loanNumber, long page, long size) {
        return gateway.listPayments(loanNumber, page, size);
    }

    @Override
    public OperationResult<Payment> getPayment(String loanNumber, long id) {
        return gateway.retrievePayment(loanNumber, id);
    }

    @Override
    public OperationResult<Boolean> deletePayment(String loanNumber, long id) {
        return gateway.deletePayment(loanNumber, id);
    }

    @Override
    public ValidationReport validatePayment(PaymentInput input) {
        return validation.validatePayment(input);
    }

    @Override
    public OperationResult<List<PaymentSummary>> listAllPayments(String loanNumber) {
        List<PaymentSummary> all = new ArrayList<>();
        long page = 1;
        while (true) {
            OperationResult<Page<PaymentSummary>> result = gateway.listPayments(loanNumber, page, PAGE_SIZE);
            if (!result.isSuccess()) {
                return OperationResult.failure(result.errorCode(), result.errorMessage(), result.transactionId());
            }
            all.addAll(result.value().items());
            if (result.value().items().isEmpty() || all.size() >= result.value().total()) {
                return OperationResult.success(all, result.transactionId());
            }
            page++;
        }
    }

    @Override
    public OperationResult<SaveResult<Payment>> addPayment(String loanNumber, PaymentInput input) {
        ValidationReport report = validatePayment(input);
        if (!report.errors().isEmpty()) {
            return invalid(report);
        }
        BigDecimal amount = new BigDecimal(input.amount().trim());
        return saved(gateway.addPayment(loanNumber, amount, input.date()));
    }

    private static OperationResult<SaveResult<Payment>> invalid(ValidationReport report) {
        return OperationResult.success(new SaveResult<>(null, report), 0);
    }

    private static OperationResult<SaveResult<Payment>> saved(OperationResult<Payment> result) {
        if (!result.isSuccess()) {
            return OperationResult.failure(result.errorCode(), result.errorMessage(), result.transactionId());
        }
        return OperationResult.success(
                new SaveResult<>(result.value(), new ValidationReport(List.of())),
                result.transactionId());
    }
}
