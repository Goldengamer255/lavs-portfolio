package lavs.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import lavs.domain.CustomerDocument;
import lavs.domain.CustomerDocumentSummary;
import lavs.domain.DocumentMetadata;
import lavs.domain.Page;
import lavs.gateway.DocumentGateway;
import lavs.gateway.OperationResult;
import org.junit.jupiter.api.Test;

class DocumentServiceTest {
    private static final DocumentMetadata METADATA =
        new DocumentMetadata("proof.pdf", "application/pdf", 120, "Identity");
    private static final CustomerDocument NEW_DOCUMENT =
        new CustomerDocument(22L, "C123", "proof.pdf", "application/pdf", 120, "Identity");

    @Test
    void failedAddLeavesOriginalUntouched() {
        FakeGateway gateway = new FakeGateway();
        gateway.addResult = OperationResult.failure(3, "Rejected", 7);

        var result = new DocumentService(gateway).replace("C123", 11, METADATA);

        assertFalse(result.isSuccess());
        assertFalse(result.isPartial());
        assertNull(result.replacement());
        assertEquals(List.of("add"), gateway.calls);
        assertEquals(3, result.errorCode());
    }

    @Test
    void failedDeleteReportsNewIdAndDoesNotRepeatAdd() {
        FakeGateway gateway = new FakeGateway();
        gateway.deleteResult = OperationResult.failure(10, "Unavailable", 8);

        var result = new DocumentService(gateway).replace("C123", 11, METADATA);

        assertTrue(result.isPartial());
        assertFalse(result.isSuccess());
        assertEquals(22L, result.replacement().id());
        assertEquals(10, result.errorCode());
        assertEquals(List.of("add", "delete:11"), gateway.calls);
    }

    @Test
    void successfulReplacementReturnsNewDocument() {
        FakeGateway gateway = new FakeGateway();

        var result = new DocumentService(gateway).replace("C123", 11, METADATA);

        assertTrue(result.isSuccess());
        assertEquals(22L, result.replacement().id());
        assertEquals(List.of("add", "delete:11"), gateway.calls);
    }

    private static final class FakeGateway implements DocumentGateway {
        private final List<String> calls = new ArrayList<>();
        private OperationResult<CustomerDocument> addResult = OperationResult.success(NEW_DOCUMENT, 6);
        private OperationResult<Boolean> deleteResult = OperationResult.success(true, 9);

        @Override
        public OperationResult<CustomerDocument> addDocument(String customerNumber, DocumentMetadata metadata) {
            calls.add("add");
            return addResult;
        }

        @Override
        public OperationResult<Boolean> deleteDocument(String customerNumber, long documentId) {
            calls.add("delete:" + documentId);
            return deleteResult;
        }

        @Override
        public OperationResult<Page<CustomerDocumentSummary>> listDocuments(
                String customerNumber, long page, long size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OperationResult<CustomerDocument> retrieveDocument(String customerNumber, long documentId) {
            throw new UnsupportedOperationException();
        }
    }
}
