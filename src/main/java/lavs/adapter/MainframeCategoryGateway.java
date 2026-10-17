package lavs.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lavs.gateway.CategoryGateway;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.Status;

/**
 * Reads configured dropdown categories through the supplied mainframe message API.
 *
 * <p>The mainframe exposes categories and category items as separate paged queries.
 * This adapter first resolves the requested category name to its numeric identifier,
 * then loads every item and returns the names in their configured display order.</p>
 */
public final class MainframeCategoryGateway implements CategoryGateway {

    private static final long PAGE_SIZE = 100;
    private static final int REJECTED = 3;
    private static final int UNAVAILABLE = 10;

    private final Connection connection;

    /**
     * @param connection authenticated mainframe connection shared by the application
     */
    public MainframeCategoryGateway(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public OperationResult<List<String>> getCategoryValues(String categoryName) {
        Objects.requireNonNull(categoryName, "categoryName");

        try {
            OperationResult<Long> category = findCategory(categoryName);
            if (!category.isSuccess()) {
                return copyFailure(category);
            }
            return loadItems(category.value());
        } catch (IOException exception) {
            return OperationResult.failure(UNAVAILABLE, transportMessage(exception), 0);
        }
    }

    /** Resolves a category name because the item-list request requires a numeric ID. */
    private OperationResult<Long> findCategory(String categoryName) throws IOException {
        long page = 1;
        long lastTransactionId = 0;

        while (true) {
            mainframe.client.messages.constantCategory.List request =
                    new mainframe.client.messages.constantCategory.List(page, PAGE_SIZE);
            Status status = request.send(connection);
            lastTransactionId = status.getTransactionId();
            if (!status.getWasSuccessful()) {
                return failureFrom(status);
            }

            mainframe.client.messages.constantCategory.List.Item[] items = request.getItems();
            for (mainframe.client.messages.constantCategory.List.Item item : items) {
                if (categoryName.equals(item.getName())) {
                    return OperationResult.success(item.getId(), lastTransactionId);
                }
            }

            if (isLastPage(page, request.getTotal(), items.length)) {
                break;
            }
            page++;
        }

        return OperationResult.failure(
                REJECTED, "Unknown category: " + categoryName, lastTransactionId);
    }

    /** Loads all item pages and applies the explicit order supplied by the mainframe. */
    private OperationResult<List<String>> loadItems(long categoryId) throws IOException {
        List<CategoryValue> values = new ArrayList<>();
        long page = 1;
        long lastTransactionId = 0;

        while (true) {
            mainframe.client.messages.constantItem.List request =
                    new mainframe.client.messages.constantItem.List(categoryId, page, PAGE_SIZE);
            Status status = request.send(connection);
            lastTransactionId = status.getTransactionId();
            if (!status.getWasSuccessful()) {
                return failureFrom(status);
            }

            mainframe.client.messages.constantItem.List.Item[] items = request.getItems();
            for (mainframe.client.messages.constantItem.List.Item item : items) {
                values.add(new CategoryValue(item.getName(), item.getOrder()));
            }

            if (isLastPage(page, request.getTotal(), items.length)) {
                break;
            }
            page++;
        }

        values.sort(Comparator.comparingLong(CategoryValue::order));
        List<String> names = values.stream().map(CategoryValue::name).toList();
        return OperationResult.success(names, lastTransactionId);
    }

    private boolean isLastPage(long page, long total, int pageItemCount) {
        return pageItemCount == 0 || page * PAGE_SIZE >= total;
    }

    private <T> OperationResult<T> failureFrom(Status status) {
        return OperationResult.failure(
                status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
    }

    private <T> OperationResult<T> copyFailure(OperationResult<?> result) {
        return OperationResult.failure(
                result.errorCode(), result.errorMessage(), result.transactionId());
    }

    private String transportMessage(IOException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Mainframe is unavailable." : message;
    }

    private record CategoryValue(String name, long order) {}
}
