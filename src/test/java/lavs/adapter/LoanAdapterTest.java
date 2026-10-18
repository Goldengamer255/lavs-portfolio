package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lavs.domain.CoBorrower;
import lavs.domain.Loan;
import lavs.domain.LoanSummary;
import lavs.domain.LoanSummaryItem;
import lavs.domain.NewLoan;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StubConnection;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.ArgumentCaptor;

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
        .thenReturn(
            new MessageResponse(
                new Status(53),
                new HashMap<>(
                    Map.of(
                        "page",
                        "2",
                        "total",
                        "4",
                        "items",
                        "1",
                        "1:loanNumber",
                        "L17",
                        "1:principal",
                        "12000.50",
                        "1:startDate",
                        "2026-03-01",
                        "1:status",
                        "Active"))));

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
        .thenReturn(
            new MessageResponse(
                new Status(54),
                new HashMap<>(
                    Map.ofEntries(
                        Map.entry("loanNumber", "L18"),
                        Map.entry("customerNumber", "C17"),
                        Map.entry("principal", "10000.0"),
                        Map.entry("paymentAmount", "250.0"),
                        Map.entry("rateAmount", "5.5"),
                        Map.entry("rateType", "Fixed"),
                        Map.entry("rateFrequency", "Yearly"),
                        Map.entry("paymentFrequency", "Monthly"),
                        Map.entry("startDate", "2026-04-01"),
                        Map.entry("renewalDate", "2027-04-01"),
                        Map.entry("status", "Pending")))));
    var input =
        new NewLoan(
            "C17",
            10000,
            250,
            5.5,
            "Fixed",
            "Yearly",
            "Monthly",
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2027, 4, 1));

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
        .thenReturn(
            new MessageResponse(
                new Status(55),
                new HashMap<>(
                    Map.of(
                        "loanNumber",
                        "L18",
                        "coborrowerNumber",
                        "C22",
                        "type",
                        "Guarantor",
                        "id",
                        "87"))));

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

  // AI-assisted: response fixtures and the operation matrix were prepared with AI support.
  private static final LocalDate START = LocalDate.of(2026, 4, 1);
  private static final LocalDate RENEWAL = LocalDate.of(2027, 4, 1);
  private static final NewLoan NEW_LOAN =
      new NewLoan("C17", 10000, 250, 5.5, "Fixed", "Yearly", "Monthly", START, RENEWAL);
  private static final Loan LOAN =
      new Loan(
          "L18", "C17", 10000, 250, 5.5, "Fixed", "Yearly", "Monthly", START, RENEWAL, "Pending");

  private record Case(
      String requestType,
      String inputKey,
      String inputValue,
      Function<LoanAdapter, OperationResult<?>> call,
      Class<?> resultType) {}

  private static List<Case> cases() {
    return List.of(
        new Case("loan.create", "startDate", "2026-04-01", a -> a.create(NEW_LOAN), Loan.class),
        new Case("loan.retrieve", "number", "L18", a -> a.retrieve("L18"), Loan.class),
        new Case(
            "loan.updateStatus",
            "status",
            "Active",
            a -> a.changeStatus("L18", "Active"),
            String.class),
        new Case(
            "loanSummary.retrieve", "number", "L18", a -> a.getSummary("L18"), LoanSummary.class),
        new Case("loan.update", "startDate", "2026-04-01", a -> a.update(LOAN), Loan.class),
        new Case(
            "loanRepaymentSchedule.list",
            "page",
            "2",
            a -> a.getRepaymentSchedule("L18", 2, 5),
            Page.class),
        new Case("loan.list", "page", "2", a -> a.listLoans("C17", 2, 5), Page.class),
        new Case(
            "loan.addCoborrower",
            "coborrowerNumber",
            "C22",
            a -> a.addCoborrower("L18", "C22", "Guarantor"),
            Long.class),
        new Case(
            "loan.deleteCoborrower", "id", "31", a -> a.deleteCoborrower("L18", 31), Boolean.class),
        new Case(
            "loanCoborrower.list", "page", "2", a -> a.listCoborrowers("L18", 2, 5), Page.class),
        new Case(
            "loanCoborrower.retrieve",
            "id",
            "31",
            a -> a.retrieveCoborrower("L18", 31),
            CoBorrower.class));
  }

  @TestFactory
  List<DynamicTest> mapsSuccessfulResponsesAndRequests() {
    List<DynamicTest> tests = new ArrayList<>();
    for (Case testCase : cases()) {
      tests.add(
          DynamicTest.dynamicTest(
              testCase.requestType(),
              () -> {
                Connection connection = mock(Connection.class);
                when(connection.send(any(MessageRequest.class)))
                    .thenReturn(new MessageResponse(new Status(71), successFields()));

                OperationResult<?> result = testCase.call().apply(new LoanAdapter(connection));

                assertTrue(result.isSuccess());
                assertEquals(71, result.transactionId());
                assertNotNull(result.value());
                assertTrue(testCase.resultType().isInstance(result.value()));
                assertMappedValue(testCase.requestType(), result.value());
                if (result.value() instanceof Page<?> page) {
                  assertEquals(2, page.page());
                  assertEquals(5, page.size());
                  assertEquals(8, page.total());
                  assertEquals(1, page.items().size());
                }
                var request = ArgumentCaptor.forClass(MessageRequest.class);
                verify(connection).send(request.capture());
                assertEquals(testCase.requestType(), request.getValue().getRequestType());
                assertEquals(
                    testCase.inputValue(), request.getValue().getValue(testCase.inputKey()));
              }));
    }
    return tests;
  }

  @TestFactory
  List<DynamicTest> preservesRejectedResponses() {
    List<DynamicTest> tests = new ArrayList<>();
    for (Case testCase : cases()) {
      tests.add(
          DynamicTest.dynamicTest(
              testCase.requestType(),
              () -> {
                Connection connection = mock(Connection.class);
                when(connection.send(any(MessageRequest.class)))
                    .thenReturn(
                        new MessageResponse(
                            new Status(3, "Rejected by mainframe", 72), new HashMap<>()));

                OperationResult<?> result = testCase.call().apply(new LoanAdapter(connection));

                assertTrue(result.isRejected());
                assertEquals("Rejected by mainframe", result.errorMessage());
                assertEquals(72, result.transactionId());
              }));
    }
    return tests;
  }

  // AI-assisted: AI recommended checking that a dropped connection becomes an unavailable result.
  @TestFactory
  List<DynamicTest> mapsTransportErrorsToUnavailable() {
    List<DynamicTest> tests = new ArrayList<>();
    for (Case testCase : cases()) {
      tests.add(
          DynamicTest.dynamicTest(
              testCase.requestType(),
              () -> {
                Connection connection = mock(Connection.class);
                when(connection.send(any(MessageRequest.class)))
                    .thenAnswer(
                        invocation -> {
                          throw new IOException("Connection lost");
                        });

                OperationResult<?> result = testCase.call().apply(new LoanAdapter(connection));

                assertTrue(result.isUnavailable());
                assertEquals("Connection lost", result.errorMessage());
                assertEquals(0, result.transactionId());
              }));
    }
    return tests;
  }

  // AI-assisted: AI helped cross-check loan summary and paged item keys against the mainframe
  // parsers.
  private static HashMap<String, String> successFields() {
    HashMap<String, String> values = new HashMap<>();
    values.putAll(
        Map.ofEntries(
            Map.entry("loanNumber", "L18"), Map.entry("customerNumber", "C17"),
            Map.entry("customerName", "Aroha Smith"), Map.entry("status", "Active"),
            Map.entry("principal", "10000"), Map.entry("paymentAmount", "250"),
            Map.entry("rateAmount", "5.5"), Map.entry("rateType", "Fixed"),
            Map.entry("rateFrequency", "Yearly"), Map.entry("paymentFrequency", "Monthly"),
            Map.entry("startDate", "2026-04-01"), Map.entry("renewalDate", "2027-04-01"),
            Map.entry("totalAmountPaid", "1500"), Map.entry("totalInterestPaid", "200"),
            Map.entry("numberOfPayments", "6"), Map.entry("finalPaymentDate", "2027-04-01"),
            Map.entry("finalPaymentAmount", "250"), Map.entry("id", "31"),
            Map.entry("coborrowerNumber", "C22"), Map.entry("coborrowerName", "Lee Wong"),
            Map.entry("type", "Guarantor"), Map.entry("page", "2"),
            Map.entry("total", "8"), Map.entry("items", "1")));
    values.putAll(
        Map.ofEntries(
            Map.entry("1:loanNumber", "L18"), Map.entry("1:principal", "10000"),
            Map.entry("1:startDate", "2026-04-01"), Map.entry("1:status", "Active"),
            Map.entry("1:paymentNumber", "3"), Map.entry("1:date", "2026-05-01"),
            Map.entry("1:amountPaid", "250"), Map.entry("1:principalAmount", "200"),
            Map.entry("1:interestAmount", "50"), Map.entry("1:amountRemaining", "9800"),
            Map.entry("1:id", "31"), Map.entry("1:coborrowerNumber", "C22"),
            Map.entry("1:coborrowerName", "Lee Wong"), Map.entry("1:type", "Guarantor")));
    return values;
  }

  private static void assertMappedValue(String requestType, Object value) {
    switch (requestType) {
      case "loan.create", "loan.retrieve", "loan.update" ->
          assertEquals(
              new Loan(
                  "L18", "C17", 10000, 250, 5.5, "Fixed", "Yearly", "Monthly", START, RENEWAL,
                  "Active"),
              value);
      case "loan.updateStatus" -> assertEquals("Active", value);
      case "loanSummary.retrieve" ->
          assertEquals(new LoanSummary("L18", "Aroha Smith", 1500, 200, 6, RENEWAL, 250), value);
      case "loanRepaymentSchedule.list" ->
          assertEquals(
              List.of(new RepaymentScheduleItem(3, LocalDate.of(2026, 5, 1), 250, 200, 50, 9800)),
              ((Page<?>) value).items());
      case "loan.list" ->
          assertEquals(
              List.of(new LoanSummaryItem("L18", 10000, START, "Active")),
              ((Page<?>) value).items());
      case "loan.addCoborrower" -> assertEquals(31L, value);
      case "loan.deleteCoborrower" -> assertEquals(Boolean.TRUE, value);
      case "loanCoborrower.list" ->
          assertEquals(
              List.of(new CoBorrower(31, "L18", "C22", "Lee Wong", "Guarantor")),
              ((Page<?>) value).items());
      case "loanCoborrower.retrieve" ->
          assertEquals(new CoBorrower(31, "L18", "C22", "Lee Wong", "Guarantor"), value);
      default -> throw new AssertionError("Missing mapping assertion for " + requestType);
    }
  }
}
