package lavs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import lavs.gateway.CategoryGateway;
import lavs.gateway.OperationResult;
import org.junit.jupiter.api.Test;

/** Tests category input handling independently of mainframe communication. */
class DefaultCategoryServiceTest {

    @Test
    void trimsCategoryNameBeforeCallingGateway() {
        RecordingCategoryGateway gateway = new RecordingCategoryGateway(
                OperationResult.success(List.of("Home", "Other"), 42));
        DefaultCategoryService service = new DefaultCategoryService(gateway);

        OperationResult<List<String>> result =
                service.getCategoryValues(" CustomerAddressType ");

        assertTrue(result.isSuccess());
        assertEquals(List.of("Home", "Other"), result.value());
        assertEquals("CustomerAddressType", gateway.requestedCategory());
        assertEquals(42, result.transactionId());
    }

    @Test
    void rejectsBlankCategoryNameWithoutCallingGateway() {
        RecordingCategoryGateway gateway = new RecordingCategoryGateway(
                OperationResult.success(List.of("unused"), 1));
        DefaultCategoryService service = new DefaultCategoryService(gateway);

        OperationResult<List<String>> result = service.getCategoryValues("   ");

        assertFalse(result.isSuccess());
        assertTrue(result.isRejected());
        assertEquals("Category name is required.", result.errorMessage());
        assertNull(gateway.requestedCategory());
    }

    @Test
    void preservesGatewayFailureForTheController() {
        RecordingCategoryGateway gateway = new RecordingCategoryGateway(
                OperationResult.failure(10, "Server unavailable", 8));
        DefaultCategoryService service = new DefaultCategoryService(gateway);

        OperationResult<List<String>> result = service.getCategoryValues("LoanRateType");

        assertTrue(result.isUnavailable());
        assertEquals("Server unavailable", result.errorMessage());
        assertEquals(8, result.transactionId());
    }

    private static final class RecordingCategoryGateway implements CategoryGateway {
        private final OperationResult<List<String>> result;
        private String requestedCategory;

        private RecordingCategoryGateway(OperationResult<List<String>> result) {
            this.result = result;
        }

        @Override
        public OperationResult<List<String>> getCategoryValues(String categoryName) {
            requestedCategory = categoryName;
            return result;
        }

        String requestedCategory() {
            return requestedCategory;
        }
    }
}
