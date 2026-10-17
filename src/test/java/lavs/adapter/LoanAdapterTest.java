package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lavs.domain.NewLoan;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StubConnection;
import org.mockito.ArgumentCaptor;
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

  // AI-assisted: mock response fields and request assertions for loan and co-borrower mapping.
  @Test
  void listLoansMapsPagePrincipalAndStartDate() {
    Connection connection = mock(Connection.class);
    when(connection.send(any(MessageRequest.class)))
        .thenReturn(new MessageResponse(new Status(53), new HashMap<>(Map.of(
            "page", "2", "total", "4", "items", "1",
            "1:loanNumber", "L17", "1:principal", "12000.50",
            "1:startDate", "2026-03-01", "1:status", "Active"))));

    var result = new LoanAdapter(connection).listLoans("C17", 2, 2);

    assertTrue(result.isSuccess());
    assertEquals(2, result.value().page());
    assertEquals(2, result.value().size());
    assertEquals(4, result.value().total());
    assertEquals("L17", result.value().items().get(0).loanNumber());
    assertEquals(12000.50, result.value().items().get(0).principal());
    assertEquals(LocalDate.of(2026, 3, 1), result.value().items().get(0).startDate());
    var request = ArgumentCaptor.forClass(MessageRequest.class);
    verify(connection).send(request.capture());
    assertEquals("loan.list", request.getValue().getRequestType());
    assertEquals("C17", request.getValue().getValue("number"));
    assertEquals("2", request.getValue().getValue("page"));
  }

  @Test
  void createLoanFormatsDatesAndMapsReturnedStatus() {
    Connection connection = mock(Connection.class);
    when(connection.send(any(MessageRequest.class)))
        .thenReturn(new MessageResponse(new Status(54), new HashMap<>(Map.ofEntries(
            Map.entry("loanNumber", "L18"), Map.entry("customerNumber", "C17"),
            Map.entry("principal", "10000.0"), Map.entry("paymentAmount", "250.0"),
            Map.entry("rateAmount", "5.5"), Map.entry("rateType", "Fixed"),
            Map.entry("rateFrequency", "Yearly"), Map.entry("paymentFrequency", "Monthly"),
            Map.entry("startDate", "2026-04-01"), Map.entry("renewalDate", "2027-04-01"),
            Map.entry("status", "Pending")))));
    var input = new NewLoan("C17", 10000, 250, 5.5, "Fixed", "Yearly", "Monthly",
        LocalDate.of(2026, 4, 1), LocalDate.of(2027, 4, 1));

    var result = new LoanAdapter(connection).create(input);

    assertTrue(result.isSuccess());
    assertEquals("L18", result.value().loanNumber());
    assertEquals("Pending", result.value().status());
    assertEquals(LocalDate.of(2027, 4, 1), result.value().renewalDate());
    var request = ArgumentCaptor.forClass(MessageRequest.class);
    verify(connection).send(request.capture());
    assertEquals("loan.create", request.getValue().getRequestType());
    assertEquals("2026-04-01", request.getValue().getValue("startDate"));
    assertEquals("2027-04-01", request.getValue().getValue("renewalDate"));
    assertEquals("C17", request.getValue().getValue("customerNumber"));
  }

  @Test
  void addCoborrowerReturnsLinkIdAndSendsBothNumbers() {
    Connection connection = mock(Connection.class);
    when(connection.send(any(MessageRequest.class)))
        .thenReturn(new MessageResponse(new Status(55), new HashMap<>(Map.of(
            "loanNumber", "L18", "coborrowerNumber", "C22", "type", "Guarantor", "id", "87"))));

    var result = new LoanAdapter(connection).addCoborrower("L18", "C22", "Guarantor");

    assertTrue(result.isSuccess());
    assertEquals(87L, result.value());
    var request = ArgumentCaptor.forClass(MessageRequest.class);
    verify(connection).send(request.capture());
    assertEquals("loan.addCoborrower", request.getValue().getRequestType());
    assertEquals("L18", request.getValue().getValue("loanNumber"));
    assertEquals("C22", request.getValue().getValue("coborrowerNumber"));
    assertEquals("Guarantor", request.getValue().getValue("type"));
  }
}
