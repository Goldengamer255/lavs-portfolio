package lavs.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import org.junit.jupiter.api.Test;

class DocumentAdapterTest {
    @Test
    void addSendsOnlyMetadataAndMapsReturnedId() {
        var connection = new RecordingConnection(new MessageResponse(new Status(12),
            new HashMap<>(Map.of(
                "customer", "C123", "id", "22", "fileName", "proof.pdf",
                "fileType", "application/pdf", "size", "120", "type", "Identity"))));

        var result = new CustomerAdapter(connection).addDocument("C123",
            new lavs.domain.DocumentMetadata("proof.pdf", "application/pdf", 120, "Identity"));

        assertTrue(result.isSuccess());
        assertEquals(22L, result.value().id());
        assertEquals("customer.addDocument", connection.request.getRequestType());
        assertEquals("C123", connection.request.getValue("customer"));
        assertEquals("120", connection.request.getValue("size"));
        assertEquals("application/pdf", connection.request.getValue("fileType"));
    }

    @Test
    void listMapsPagedSummaries() {
        var connection = new RecordingConnection(new MessageResponse(new Status(13),
            new HashMap<>(Map.of(
                "page", "2", "total", "6", "items", "1", "1:id", "22",
                "1:fileName", "proof.pdf", "1:type", "Identity"))));

        var result = new CustomerAdapter(connection).listDocuments("C123", 2, 5);

        assertTrue(result.isSuccess());
        assertEquals(6, result.value().total());
        assertEquals(2, result.value().page());
        assertEquals(5, result.value().size());
        assertEquals(22L, result.value().items().get(0).id());
        assertEquals("customerDocument.list", connection.request.getRequestType());
    }

    private static final class RecordingConnection implements Connection {
        private final MessageResponse response;
        private MessageRequest request;

        private RecordingConnection(MessageResponse response) {
            this.response = response;
        }

        @Override public MessageResponse send(MessageRequest request) {
            this.request = request;
            return response;
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() {}
        @Override public Status Login(String username, String password) { throw new UnsupportedOperationException(); }
        @Override public void Logout() {}
    }
}
