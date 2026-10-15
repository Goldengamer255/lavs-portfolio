package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import org.junit.jupiter.api.Test;

/** Verifies category and item message translation without contacting the mainframe. */
class MainframeCategoryGatewayTest {

    @Test
    void resolvesCategoryAndReturnsValuesInConfiguredOrder() {
        ScriptedConnection connection = new ScriptedConnection();
        connection.enqueue("constantCategory.list", success(11, values(
                "page", "1", "total", "1", "items", "1",
                "1:id", "27", "1:name", "CustomerAddressType")));
        connection.enqueue("constantItem.list", success(12, values(
                "categoryId", "27", "categoryName", "CustomerAddressType",
                "page", "1", "total", "2", "items", "2",
                "1:name", "Other", "1:order", "2",
                "2:name", "Home", "2:order", "1")));

        MainframeCategoryGateway gateway = new MainframeCategoryGateway(connection);

        OperationResult<List<String>> result =
                gateway.getCategoryValues("CustomerAddressType");

        assertTrue(result.isSuccess());
        assertEquals(List.of("Home", "Other"), result.value());
        assertEquals(12, result.transactionId());
        assertEquals("27", connection.requests().get(1).getValue("category"));
    }

    @Test
    void returnsRejectedResultWhenCategoryDoesNotExist() {
        ScriptedConnection connection = new ScriptedConnection();
        connection.enqueue("constantCategory.list", success(21, values(
                "page", "1", "total", "0", "items", "0")));
        MainframeCategoryGateway gateway = new MainframeCategoryGateway(connection);

        OperationResult<List<String>> result = gateway.getCategoryValues("MissingType");

        assertFalse(result.isSuccess());
        assertTrue(result.isRejected());
        assertEquals("Unknown category: MissingType", result.errorMessage());
        assertEquals(21, result.transactionId());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void preservesMainframeFailureWhenCategoryLookupFails() {
        ScriptedConnection connection = new ScriptedConnection();
        connection.enqueue("constantCategory.list",
                new MessageResponse(new Status(10, "Server unavailable", 31), new HashMap<>()));
        MainframeCategoryGateway gateway = new MainframeCategoryGateway(connection);

        OperationResult<List<String>> result =
                gateway.getCategoryValues("CustomerAddressType");

        assertFalse(result.isSuccess());
        assertTrue(result.isUnavailable());
        assertEquals("Server unavailable", result.errorMessage());
        assertEquals(31, result.transactionId());
    }

    private static MessageResponse success(long transactionId, HashMap<String, String> values) {
        return new MessageResponse(new Status(transactionId), values);
    }

    private static HashMap<String, String> values(String... entries) {
        HashMap<String, String> values = new HashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put(entries[index], entries[index + 1]);
        }
        return values;
    }

    /** Connection stub that verifies the mainframe messages are sent in order. */
    private static final class ScriptedConnection implements Connection {
        private final Queue<ExpectedResponse> responses = new ArrayDeque<>();
        private final java.util.ArrayList<MessageRequest> requests = new java.util.ArrayList<>();

        void enqueue(String requestType, MessageResponse response) {
            responses.add(new ExpectedResponse(requestType, response));
        }

        List<MessageRequest> requests() {
            return List.copyOf(requests);
        }

        @Override
        public MessageResponse send(MessageRequest request) {
            ExpectedResponse expected = responses.remove();
            assertEquals(expected.requestType(), request.getRequestType());
            requests.add(request);
            return expected.response();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            responses.clear();
        }
    }

    private record ExpectedResponse(String requestType, MessageResponse response) {}
}
