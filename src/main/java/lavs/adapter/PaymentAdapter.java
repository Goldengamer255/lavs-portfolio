package lavs.adapter;

import lavs.domain.Page;
import lavs.domain.Payment;
import lavs.domain.PaymentSummary;
import lavs.gateway.OperationResult;
import lavs.gateway.PaymentGateway;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import mainframe.client.messages.loan.AddPayment;
import mainframe.client.messages.loan.DeletePayment;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// AI-assisted: retrievePayment, addPayment, listPayments and toMoney structure influenced by AI; adapted from NoteAdapter
/**
 * Sends loan payment requests to the mainframe and turns the replies into domain records.
 *
 * <p>Each method sends one mainframe message and returns an OperationResult, so no mainframe
 * type reaches the business logic layer. A failed Status becomes a failure result, and a
 * network error (IOException) becomes the "unavailable" failure code.
 *
 * <p>The mainframe sends amounts as doubles and dates as text, so amounts are converted to
 * BigDecimal (a missing amount counts as zero) and dates are parsed to LocalDate.
 * Dates are sent back to the mainframe as yyyy-MM-dd.
 *
 * <p>The mainframe has no update message for payments, so only list, retrieve, add and
 * delete are supported.
 */
public class PaymentAdapter implements PaymentGateway {

    private final mainframe.client.Connection connection;

    public PaymentAdapter(mainframe.client.Connection connection) { this.connection = connection; }

    @Override
    public OperationResult<Boolean> deletePayment(String loanNumber, long id) {
        DeletePayment message = new DeletePayment(id, loanNumber);

        try {
            Status status = message.send(connection);
            if (!status.getWasSuccessful()) {
                return OperationResult.failure(status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
            } else {
                return OperationResult.success(true, status.getTransactionId());
            }
        } catch (IOException e) {
            return OperationResult.failure(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
        }
    }

    @Override
    public OperationResult<Payment> retrievePayment(String loanNumber, long id) {
        var message = new mainframe.client.messages.loanPayment.Retrieve(id, loanNumber);
        try {
            Status status = message.send(connection);
            if (!status.getWasSuccessful()) {
                return OperationResult.failure(
                        status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
            }
            Payment payment = new Payment(
                    id,
                    loanNumber,
                    NoteAdapter.parseDate(message.getDate()),
                    toMoney(message.getAmountPaid()),
                    toMoney(message.getPrincipalAmount()),
                    toMoney(message.getInterestAmount()),
                    toMoney(message.getAmountRemaining()));
            return OperationResult.success(payment, status.getTransactionId());
        } catch (IOException e) {
            return OperationResult.failure(
                    StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
        }
    }

    @Override
    public OperationResult<Payment> addPayment(String loanNumber, BigDecimal amountPaid, LocalDate date) {
        var message = new AddPayment(amountPaid.doubleValue(), date.toString(), loanNumber);
        try {
            Status status = message.send(connection);
            if (!status.getWasSuccessful()) {
                return OperationResult.failure(
                        status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
            }
            Payment payment = new Payment(
                    message.getId() == null ? 0 : message.getId(),
                    loanNumber,
                    NoteAdapter.parseDate(message.getDate()),
                    toMoney(message.getAmountPaid()),
                    toMoney(message.getPrincipalAmount()),
                    toMoney(message.getInterestAmount()),
                    toMoney(message.getAmountRemaining()));
            return OperationResult.success(payment, status.getTransactionId());
        } catch (IOException e) {
            return OperationResult.failure(
                    StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
        }


    }

    @Override
    public OperationResult<Page<PaymentSummary>> listPayments(String loanNumber, long page, long size) {
        var message = new mainframe.client.messages.loanPayment.List(loanNumber, page, size);
        try {
            Status status = message.send(connection);
            if (!status.getWasSuccessful()) {
                return OperationResult.failure(
                        status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
            }
            List<PaymentSummary> payments = new ArrayList<>();
            for (var item : message.getItems()) {
                payments.add(new PaymentSummary(
                        item.getId(),
                        NoteAdapter.parseDate(item.getDate()),
                        toMoney(item.getAmountPaid())));
            }
            return OperationResult.success(
                    new Page<>(payments, message.getPage(), size, message.getTotal()),
                    status.getTransactionId());
        } catch (IOException e) {
            return OperationResult.failure(
                    StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
        }
    }

    // The mainframe sends amounts as doubles; convert to BigDecimal, treating a missing value as zero.
    private static BigDecimal toMoney(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

}
