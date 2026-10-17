package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StubConnection;
import org.junit.jupiter.api.Test;

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
}
