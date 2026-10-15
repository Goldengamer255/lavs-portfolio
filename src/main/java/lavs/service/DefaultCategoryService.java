package lavs.service;

import java.util.List;
import java.util.Objects;
import lavs.gateway.CategoryGateway;
import lavs.gateway.OperationResult;

/** Default business service for dropdown values configured on the mainframe. */
public final class DefaultCategoryService implements CategoryService {

    private static final int REJECTED = 3;
    private static final String REQUIRED_MESSAGE = "Category name is required.";

    private final CategoryGateway gateway;

    /**
     * @param gateway category persistence boundary
     */
    public DefaultCategoryService(CategoryGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    @Override
    public OperationResult<List<String>> getCategoryValues(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return OperationResult.failure(REJECTED, REQUIRED_MESSAGE, 0);
        }
        return gateway.getCategoryValues(categoryName.trim());
    }
}
