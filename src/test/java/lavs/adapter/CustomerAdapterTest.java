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
import lavs.domain.Address;
import lavs.domain.AddressSummary;
import lavs.domain.Customer;
import lavs.domain.CustomerSummary;
import lavs.domain.Email;
import lavs.domain.EmailSummary;
import lavs.domain.Employer;
import lavs.domain.EmployerSummary;
import lavs.domain.NewCustomer;
import lavs.domain.Page;
import lavs.domain.Phone;
import lavs.domain.PhoneSummary;
import lavs.domain.Relationship;
import lavs.domain.RelationshipSummary;
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

class CustomerAdapterTest {

  // Ai assisted: mocked values made up by ai
  @Test
  void changeStatusReturnsTheEchoedStatusRatherThanTheCustomerNumber() {
    var connection = new StubConnection();
    connection.setResponse(
        new MessageResponse(
            new Status(41),
            new HashMap<>(java.util.Map.of("customer", "C123", "status", "Inactive"))));

    var result = new CustomerAdapter(connection).changeStatus("C123", "Inactive");

    assertTrue(result.isSuccess());
    assertEquals("Inactive", result.value());
    assertEquals(41, result.transactionId());
  }

  @Test
  void listAddressesMapsPageAndMainframeFlags() {
    var connection = new StubConnection();
    connection.setResponse(
        new MessageResponse(
            new Status(42),
            new HashMap<>(
                java.util.Map.of(
                    "page", "2",
                    "total", "7",
                    "items", "1",
                    "1:id", "19",
                    "1:city", "Auckland",
                    "1:country", "NZ",
                    "1:type", "Residential",
                    "1:isMailing", "n",
                    "1:isPrimary", "y"))));

    var result = new CustomerAdapter(connection).listAddresses("C123", 2, 5);

    assertTrue(result.isSuccess());
    assertEquals(2, result.value().page());
    assertEquals(5, result.value().size());
    assertEquals(7, result.value().total());
    assertEquals(1, result.value().items().size());
    var address = result.value().items().get(0);
    assertEquals(19L, address.id());
    assertEquals("Auckland", address.city());
    assertFalse(address.isMailing());
    assertTrue(address.isPrimary());
  }

  @Test
  void rejectedCustomerRequestPreservesTheMainframeError() {
    var connection = new StubConnection();
    connection.setResponse(
        new MessageResponse(new Status(3, "Customer not found", 43), new HashMap<>()));

    var result = new CustomerAdapter(connection).retrieve("C123");

    assertFalse(result.isSuccess());
    assertNull(result.value());
    assertEquals(3, result.errorCode());
    assertEquals("Customer not found", result.errorMessage());
    assertEquals(43, result.transactionId());
  }

  @Test
  void listRelationshipsMapsMainframeSummaries() {
    var connection = new StubConnection();
    connection.setResponse(
        new MessageResponse(
            new Status(43),
            new HashMap<>(
                java.util.Map.of(
                    "customerNumber", "C1",
                    "customerName", "Ana Singh",
                    "page", "1",
                    "total", "1",
                    "items", "1",
                    "1:id", "8",
                    "1:secondaryCustomerNumber", "C2",
                    "1:type", "Spouse"))));

    var result = new CustomerAdapter(connection).listRelationships("C1", 1, 50);

    assertTrue(result.isSuccess());
    assertEquals(1, result.value().total());
    assertEquals(8L, result.value().items().get(0).id());
    assertEquals("C2", result.value().items().get(0).secondaryCustomerNumber());
    assertEquals("Spouse", result.value().items().get(0).type());
  }

  @Test
  void retrieveRelationshipMapsCustomerNamesAndOtherType() {
    var connection = new StubConnection();
    connection.setResponse(
        new MessageResponse(
            new Status(44),
            new HashMap<>(
                java.util.Map.of(
                    "customerNumber", "C1",
                    "customerName", "Ana Singh",
                    "id", "9",
                    "secondaryCustomerNumber", "C3",
                    "secondaryCustomerName", "Lee Wong",
                    "type", "Other",
                    "otherType", "Sibling"))));

    var result = new CustomerAdapter(connection).retrieveRelationship("C1", 9);

    assertTrue(result.isSuccess());
    assertEquals("Ana Singh", result.value().customerName());
    assertEquals("Lee Wong", result.value().secondaryCustomerName());
    assertEquals("Sibling", result.value().otherType());
    assertEquals(44, result.transactionId());
  }

  // AI-assisted: mock response fields and request assertions for search and phone mapping.
  @Test
  void searchSendsPageAndMapsCustomerDates() {
    Connection connection = mock(Connection.class);
    when(connection.send(any(MessageRequest.class)))
        .thenReturn(
            new MessageResponse(
                new Status(45),
                new HashMap<>(
                    Map.of(
                        "page",
                        "3",
                        "total",
                        "12",
                        "items",
                        "1",
                        "1:number",
                        "C17",
                        "1:firstName",
                        "Aroha",
                        "1:familyName",
                        "Smith",
                        "1:dateOfBirth",
                        "1998-04-12",
                        "1:title",
                        "Ms",
                        "1:status",
                        "Active"))));

    var result = new CustomerAdapter(connection).search("Smith", 3, 5);

    assertTrue(result.isSuccess());
    assertEquals(3, result.value().page());
    assertEquals(5, result.value().size());
    assertEquals(12, result.value().total());
    assertEquals("C17", result.value().items().get(0).number());
    assertEquals(LocalDate.of(1998, 4, 12), result.value().items().get(0).dateOfBirth());
    var request = ArgumentCaptor.forClass(MessageRequest.class);
    verify(connection).send(request.capture());
    assertEquals("customer.search", request.getValue().getRequestType());
    assertEquals("Smith", request.getValue().getValue("search"));
    assertEquals("3", request.getValue().getValue("page"));
    assertEquals("5", request.getValue().getValue("size"));
  }

  @Test
  void addPhoneSerializesFlagsAndMapsReturnedNumber() {
    Connection connection = mock(Connection.class);
    when(connection.send(any(MessageRequest.class)))
        .thenReturn(
            new MessageResponse(
                new Status(46),
                new HashMap<>(
                    Map.of(
                        "customer",
                        "C17",
                        "id",
                        "31",
                        "countryCode",
                        "+64",
                        "number",
                        "211234567",
                        "type",
                        "Mobile",
                        "otherType",
                        "",
                        "canTxt",
                        "y",
                        "isPrimary",
                        "n"))));
    var phone = new Phone(null, true, "+64", null, "C17", false, "211234567", "", "Mobile");

    var result = new CustomerAdapter(connection).addPhone(phone);

    assertTrue(result.isSuccess());
    assertEquals(31L, result.value().id());
    assertTrue(result.value().canTxt());
    assertFalse(result.value().isPrimary());
    assertEquals("211234567", result.value().number());
    var request = ArgumentCaptor.forClass(MessageRequest.class);
    verify(connection).send(request.capture());
    assertEquals("customer.addPhoneNumber", request.getValue().getRequestType());
    assertEquals("C17", request.getValue().getValue("customer"));
    assertEquals("y", request.getValue().getValue("canTxt"));
    assertEquals("n", request.getValue().getValue("isPrimary"));
  }

  // AI-assisted: response fixtures and the operation matrix were prepared with AI support.
  private static final LocalDate BIRTH_DATE = LocalDate.of(1998, 4, 12);
  private static final Customer CUSTOMER =
      new Customer("C17", "Aroha", "Smith", BIRTH_DATE, "NZ", "Ms", "Active");
  private static final Address ADDRESS =
      new Address(
          31L,
          "C17",
          "1 Queen St",
          "Unit 2",
          "CBD",
          "Auckland",
          "NZ",
          "1010",
          "Home",
          "",
          true,
          false);
  private static final Phone PHONE =
      new Phone(31L, true, "+64", null, "C17", false, "021123456", "", "Mobile");
  private static final Email EMAIL =
      new Email(31L, null, "C17", "aroha@example.com", false, "", "Personal");
  private static final Employer EMPLOYER =
      new Employer(31L, null, "C17", "Acme", "Engineer", 85000, "Current");
  private static final Relationship RELATIONSHIP =
      new Relationship(31L, "C17", null, "C22", null, "Spouse", "");

  private record Case(
      String requestType,
      String inputKey,
      String inputValue,
      Function<CustomerAdapter, OperationResult<?>> call,
      Class<?> resultType) {}

  private static List<Case> cases() {
    return List.of(
        new Case(
            "customer.create",
            "organisation",
            "2",
            a -> a.create(new NewCustomer("Aroha", "Smith", BIRTH_DATE, "NZ", 2, "Ms")),
            Customer.class),
        new Case("customer.retrieve", "number", "C17", a -> a.retrieve("C17"), Customer.class),
        new Case("customer.search", "page", "2", a -> a.search("Smith", 2, 5), Page.class),
        new Case("customer.update", "number", "C17", a -> a.update(CUSTOMER), Customer.class),
        new Case(
            "customer.updateStatus",
            "status",
            "Inactive",
            a -> a.changeStatus("C17", "Inactive"),
            String.class),
        new Case(
            "customer.addAddress",
            "line1",
            "1 Queen St",
            a -> a.addAddress(ADDRESS),
            Address.class),
        new Case(
            "customer.deleteAddress", "id", "31", a -> a.deleteAddress("C17", 31), Boolean.class),
        new Case(
            "customer.updateAddress", "id", "31", a -> a.updateAddress(ADDRESS), Address.class),
        new Case(
            "customerAddress.list", "page", "2", a -> a.listAddresses("C17", 2, 5), Page.class),
        new Case(
            "customerAddress.retrieve",
            "id",
            "31",
            a -> a.retrieveAddress("C17", 31),
            Address.class),
        new Case("customer.addPhoneNumber", "canTxt", "y", a -> a.addPhone(PHONE), Phone.class),
        new Case(
            "customer.deletePhoneNumber", "id", "31", a -> a.deletePhone("C17", 31), Boolean.class),
        new Case("customer.updatePhoneNumber", "id", "31", a -> a.updatePhone(PHONE), Phone.class),
        new Case(
            "customerPhoneNumber.list", "page", "2", a -> a.listPhones("C17", 2, 5), Page.class),
        new Case(
            "customerPhoneNumber.retrieve",
            "id",
            "31",
            a -> a.retrievePhone("C17", 31),
            Phone.class),
        new Case(
            "customer.addEmail",
            "address",
            "aroha@example.com",
            a -> a.addEmail(EMAIL),
            Email.class),
        new Case("customer.deleteEmail", "id", "31", a -> a.deleteEmail("C17", 31), Boolean.class),
        new Case("customer.updateEmail", "id", "31", a -> a.updateEmail(EMAIL), Email.class),
        new Case("customerEmail.list", "page", "2", a -> a.listEmails("C17", 2, 5), Page.class),
        new Case(
            "customerEmail.retrieve", "id", "31", a -> a.retrieveEmail("C17", 31), Email.class),
        new Case(
            "customer.addEmployer", "name", "Acme", a -> a.addEmployer(EMPLOYER), Employer.class),
        new Case(
            "customer.deleteEmployer", "id", "31", a -> a.deleteEmployer("C17", 31), Boolean.class),
        new Case(
            "customer.updateEmployer", "id", "31", a -> a.updateEmployer(EMPLOYER), Employer.class),
        new Case(
            "customerEmployer.list", "page", "2", a -> a.listEmployers("C17", 2, 5), Page.class),
        new Case(
            "customerEmployer.retrieve",
            "id",
            "31",
            a -> a.retrieveEmployer("C17", 31),
            Employer.class),
        new Case(
            "customer.addRelationship",
            "secondaryCustomerNumber",
            "C22",
            a -> a.addRelationship(RELATIONSHIP),
            Relationship.class),
        new Case(
            "customer.deleteRelationship",
            "id",
            "31",
            a -> a.deleteRelationship("C17", 31),
            Boolean.class),
        new Case(
            "customer.updateRelationship",
            "id",
            "31",
            a -> a.updateRelationship(RELATIONSHIP),
            Relationship.class),
        new Case(
            "customerRelationship.list",
            "page",
            "2",
            a -> a.listRelationships("C17", 2, 5),
            Page.class),
        new Case(
            "customerRelationship.retrieve",
            "id",
            "31",
            a -> a.retrieveRelationship("C17", 31),
            Relationship.class));
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
                    .thenReturn(new MessageResponse(new Status(61), successFields()));

                OperationResult<?> result = testCase.call().apply(new CustomerAdapter(connection));

                assertTrue(result.isSuccess());
                assertEquals(61, result.transactionId());
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
                            new Status(3, "Rejected by mainframe", 62), new HashMap<>()));

                OperationResult<?> result = testCase.call().apply(new CustomerAdapter(connection));

                assertTrue(result.isRejected());
                assertEquals("Rejected by mainframe", result.errorMessage());
                assertEquals(62, result.transactionId());
              }));
    }
    return tests;
  }

  // AI-assisted: AI recommended checking that an IOException becomes an unavailable result for
  // every operation.
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

                OperationResult<?> result = testCase.call().apply(new CustomerAdapter(connection));

                assertTrue(result.isUnavailable());
                assertEquals("Connection lost", result.errorMessage());
                assertEquals(0, result.transactionId());
              }));
    }
    return tests;
  }

  // AI-assisted: AI helped cross-check reply keys, including paged items, against the mainframe
  // parsers.
  private static HashMap<String, String> successFields() {
    HashMap<String, String> values = new HashMap<>();
    values.putAll(
        Map.ofEntries(
            Map.entry("number", "C17"),
            Map.entry("customer", "C17"),
            Map.entry("customerNumber", "C17"),
            Map.entry("customerName", "Aroha Smith"),
            Map.entry("id", "31"),
            Map.entry("firstName", "Aroha"),
            Map.entry("familyName", "Smith"),
            Map.entry("dateOfBirth", "1998-04-12"),
            Map.entry("nationality", "NZ"),
            Map.entry("title", "Ms"),
            Map.entry("status", "Inactive"),
            Map.entry("type", "Home"),
            Map.entry("otherType", ""),
            Map.entry("line1", "1 Queen St"),
            Map.entry("line2", "Unit 2"),
            Map.entry("suburb", "CBD"),
            Map.entry("city", "Auckland"),
            Map.entry("country", "NZ"),
            Map.entry("postCode", "1010"),
            Map.entry("isMailing", "y"),
            Map.entry("isPrimary", "n"),
            Map.entry("canTxt", "y"),
            Map.entry("countryCode", "+64"),
            Map.entry("address", "aroha@example.com"),
            Map.entry("name", "Acme"),
            Map.entry("position", "Engineer"),
            Map.entry("salary", "85000"),
            Map.entry("secondaryCustomerNumber", "C22"),
            Map.entry("secondaryCustomerName", "Lee Wong"),
            Map.entry("page", "2"),
            Map.entry("total", "8"),
            Map.entry("items", "1")));
    values.putAll(
        Map.ofEntries(
            Map.entry("1:id", "31"),
            Map.entry("1:number", "C17"),
            Map.entry("1:firstName", "Aroha"),
            Map.entry("1:familyName", "Smith"),
            Map.entry("1:dateOfBirth", "1998-04-12"),
            Map.entry("1:title", "Ms"),
            Map.entry("1:status", "Active"),
            Map.entry("1:city", "Auckland"),
            Map.entry("1:country", "NZ"),
            Map.entry("1:type", "Home"),
            Map.entry("1:isMailing", "y"),
            Map.entry("1:isPrimary", "n"),
            Map.entry("1:canTxt", "y"),
            Map.entry("1:countryCode", "+64"),
            Map.entry("1:address", "aroha@example.com"),
            Map.entry("1:name", "Acme"),
            Map.entry("1:secondaryCustomerNumber", "C22")));
    return values;
  }

  private static void assertMappedValue(String requestType, Object value) {
    switch (requestType) {
      case "customer.create", "customer.retrieve", "customer.update" ->
          assertEquals(
              new Customer("C17", "Aroha", "Smith", BIRTH_DATE, "NZ", "Ms", "Inactive"), value);
      case "customer.updateStatus" -> assertEquals("Inactive", value);
      case "customer.search" ->
          assertEquals(
              List.of(new CustomerSummary("C17", "Aroha", "Smith", BIRTH_DATE, "Ms", "Active")),
              ((Page<?>) value).items());
      case "customer.addAddress", "customer.updateAddress", "customerAddress.retrieve" ->
          assertEquals(ADDRESS, value);
      case "customerAddress.list" ->
          assertEquals(
              List.of(new AddressSummary(31L, "Auckland", "NZ", "Home", true, false)),
              ((Page<?>) value).items());
      case "customer.addPhoneNumber", "customer.updatePhoneNumber" ->
          assertEquals(new Phone(31L, true, "+64", null, "C17", false, "C17", "", "Home"), value);
      case "customerPhoneNumber.retrieve" ->
          assertEquals(
              new Phone(31L, true, "+64", "Aroha Smith", "C17", false, "C17", "", "Home"), value);
      case "customerPhoneNumber.list" ->
          assertEquals(
              List.of(new PhoneSummary(31L, "+64", "C17", "Home", true, false)),
              ((Page<?>) value).items());
      case "customer.addEmail", "customer.updateEmail" ->
          assertEquals(new Email(31L, null, "C17", "aroha@example.com", false, "", "Home"), value);
      case "customerEmail.retrieve" ->
          assertEquals(
              new Email(31L, "Aroha Smith", "C17", "aroha@example.com", false, "", "Home"), value);
      case "customerEmail.list" ->
          assertEquals(
              List.of(new EmailSummary(31L, "aroha@example.com", false, "Home")),
              ((Page<?>) value).items());
      case "customer.addEmployer", "customer.updateEmployer" ->
          assertEquals(new Employer(31L, null, "C17", "Acme", "Engineer", 85000, "Home"), value);
      case "customerEmployer.retrieve" ->
          assertEquals(
              new Employer(31L, "Aroha Smith", "C17", "Acme", "Engineer", 85000, "Home"), value);
      case "customerEmployer.list" ->
          assertEquals(
              List.of(new EmployerSummary(31L, "Acme", "Home")), ((Page<?>) value).items());
      case "customer.addRelationship", "customer.updateRelationship" ->
          assertEquals(new Relationship(31L, "C17", null, "C22", null, "Home", ""), value);
      case "customerRelationship.retrieve" ->
          assertEquals(
              new Relationship(31L, "C17", "Aroha Smith", "C22", "Lee Wong", "Home", ""), value);
      case "customerRelationship.list" ->
          assertEquals(
              List.of(new RelationshipSummary(31L, "C22", "Home")), ((Page<?>) value).items());
      default -> assertEquals(Boolean.TRUE, value);
    }
  }
}
