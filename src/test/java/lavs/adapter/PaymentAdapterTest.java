package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import lavs.domain.Page;
import lavs.domain.Payment;
import lavs.domain.PaymentSummary;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Test;

// AI-assisted: test structure and FakeConnection adapted from NoteAdapterTest with help from AI
/**
 * Checks the payment messages sent and how replies are read, without a real mainframe.
 */
class PaymentAdapterTest {

    private final FakeConnection connection = new FakeConnection();
    private final PaymentAdapter adapter = new PaymentAdapter(connection);

    @Test
    void deleteReturnsTrue() {
        connection.reply(success(15, "id", "8", "loanNumber", "L1"));

        OperationResult<Boolean> result = adapter.deletePayment("L1", 8);

        assertTrue(result.value());
        assertEquals("loan.deletePayment", connection.sent.get(0).getRequestType());
        assertEquals("8", connection.sent.get(0).getValue("id"));
        assertEquals("L1", connection.sent.get(0).getValue("loanNumber"));
    }

    @Test
    void deleteRejectionIsPassedBack() {
        connection.reply(new MessageResponse(new Status(3, "Payment not found", 16), new HashMap<>()));

        OperationResult<Boolean> result = adapter.deletePayment("L1", 8);

        assertTrue(result.isRejected());
        assertEquals("Payment not found", result.errorMessage());
    }

    @Test
    void retrieveReturnsTheFullPayment() {
        connection.reply(success(12, "date", "2026-10-17", "amountPaid", "100.0",
                "principalAmount", "60.0", "interestAmount", "40.0", "amountRemaining", "900.0"));

        OperationResult<Payment> result = adapter.retrievePayment("L1", 4);

        assertEquals(new Payment(4, "L1", LocalDate.of(2026, 10, 17), BigDecimal.valueOf(100.0),
                        BigDecimal.valueOf(60.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(900.0)),
                result.value());
        assertEquals("loanPayment.retrieve", connection.sent.get(0).getRequestType());
        assertEquals("4", connection.sent.get(0).getValue("id"));
        assertEquals("L1", connection.sent.get(0).getValue("number"));
    }

    @Test
    void retrieveRejectionIsPassedBack() {
        connection.reply(new MessageResponse(new Status(3, "Payment not found", 16), new HashMap<>()));

        OperationResult<Payment> result = adapter.retrievePayment("L1", 1);

        assertTrue(result.isRejected());
        assertEquals("Payment not found", result.errorMessage());
    }

    @Test
    void addSendsAmountAndDate() {
        connection.reply(success(13, "id", "8", "date", "2026-10-17", "amountPaid", "100.0",
                "principalAmount", "60.0", "interestAmount", "40.0", "amountRemaining", "900.0"));

        OperationResult<Payment> result = adapter.addPayment(
                "L1", BigDecimal.valueOf(100.0), LocalDate.of(2026, 10, 17));

        assertEquals(8, result.value().id());
        assertEquals(LocalDate.of(2026, 10, 17), result.value().date());
        assertEquals("loan.addPayment", connection.sent.get(0).getRequestType());
        assertEquals("100.0", connection.sent.get(0).getValue("amountPaid"));
        assertEquals("2026-10-17", connection.sent.get(0).getValue("date"));
        assertEquals("L1", connection.sent.get(0).getValue("loanNumber"));
    }

    @Test
    void addRejectionIsPassedBack() {
        connection.reply(new MessageResponse(new Status(3, "Invalid payment", 17), new HashMap<>()));

        OperationResult<Payment> result = adapter.addPayment(
                "L1", BigDecimal.valueOf(100.0), LocalDate.of(2026, 10, 17));

        assertTrue(result.isRejected());
        assertEquals("Invalid payment", result.errorMessage());
    }

    @Test
    void listReadsEachPayment() {
        connection.reply(success(11, "page", "1", "total", "1", "items", "1",
                "1:id", "4", "1:date", "2026-10-17", "1:amountPaid", "100.0"));

        OperationResult<Page<PaymentSummary>> result = adapter.listPayments("L1", 1, 50);

        assertEquals(1, result.value().total());
        assertEquals(new PaymentSummary(4, LocalDate.of(2026, 10, 17), BigDecimal.valueOf(100.0)),
                result.value().items().get(0));
        assertEquals("loanPayment.list", connection.sent.get(0).getRequestType());
        assertEquals("L1", connection.sent.get(0).getValue("number"));
    }

    @Test
    void connectionFailureIsUnavailable() {
        connection.reply(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(0));

        OperationResult<Page<PaymentSummary>> result = adapter.listPayments("L1", 1, 50);

        assertTrue(result.isUnavailable());
    }

    private static MessageResponse success(long transactionId, String... entries) {
        HashMap<String, String> values = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            values.put(entries[i], entries[i + 1]);
        }
        return new MessageResponse(new Status(transactionId), values);
    }

    @Test
    void ioFailureIsUnavailableForEveryCall() {
        // Arrange: a connection that always fails with an IOException
        Connection broken = mock(Connection.class);
        when(broken.send(any())).thenAnswer(call -> {
            throw new IOException("down");
        });
        PaymentAdapter brokenAdapter = new PaymentAdapter(broken);

        // Act and Assert
        assertTrue(brokenAdapter.deletePayment("L1", 8).isUnavailable());
        assertTrue(brokenAdapter.retrievePayment("L1", 4).isUnavailable());
        assertTrue(brokenAdapter.addPayment("L1", BigDecimal.valueOf(100), LocalDate.of(2026, 10, 17)).isUnavailable());
        assertTrue(brokenAdapter.listPayments("L1", 1, 50).isUnavailable());
    }

    /** Returns queued replies in order and remembers what was sent. */
    private static final class FakeConnection implements Connection {
        private final Queue<MessageResponse> replies = new ArrayDeque<>();
        private final List<MessageRequest> sent = new ArrayList<>();

        void reply(MessageResponse response) {
            replies.add(response);
        }

        @Override
        public MessageResponse send(MessageRequest request) {
            sent.add(request);
            return replies.remove();
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public Status Login(String username, String password) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void Logout() {
        }

        @Override
        public void close() {
        }
    }
}