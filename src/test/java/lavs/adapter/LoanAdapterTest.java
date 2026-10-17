package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StubConnection;
import org.junit.jupiter.api.Test;

class LoanAdapterTest {

  // Ai assisted: mocked values made up by ai
  @Test
  void repaymentScheduleMapsDatesAmountsAndPage() {
    var connection = new StubConnection();
    connection.setResponse(
        new MessageResponse(
            new Status(51),
            new HashMap<>(
                java.util.Map.of(
                    "page", "2",
                    "total", "12",
                    "items", "1",
                    "1:paymentNumber", "3",
                    "1:date", "2026-11-15",
                    "1:amountPaid", "250.00",
                    "1:principalAmount", "200.00",
                    "1:interestAmount", "50.00",
                    "1:amountRemaining", "800.00"))));

    var result = new LoanAdapter(connection).getRepaymentSchedule("L123", 2, 2);

    assertTrue(result.isSuccess());
    assertEquals(51, result.transactionId());
    assertEquals(2, result.value().page());
    assertEquals(2, result.value().size());
    assertEquals(12, result.value().total());
    assertEquals(1, result.value().items().size());
    var payment = result.value().items().get(0);
    assertEquals(3, payment.paymentNumber());
    assertEquals(LocalDate.of(2026, 11, 15), payment.date());
    assertEquals(250.00, payment.amountPaid());
    assertEquals(200.00, payment.principalAmount());
    assertEquals(50.00, payment.interestAmount());
    assertEquals(800.00, payment.amountRemaining());
  }

  @Test
  void rejectedLoanStatusChangePreservesTheMainframeError() {
    var connection = new StubConnection();
    connection.setResponse(
        new MessageResponse(new Status(3, "Status change rejected", 52), new HashMap<>()));

    var result = new LoanAdapter(connection).changeStatus("L123", "Active");

    assertFalse(result.isSuccess());
    assertTrue(result.isRejected());
    assertNull(result.value());
    assertEquals(3, result.errorCode());
    assertEquals("Status change rejected", result.errorMessage());
    assertEquals(52, result.transactionId());
  }
}
