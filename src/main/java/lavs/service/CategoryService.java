package lavs.service;

import java.util.List;
import lavs.gateway.OperationResult;

/** Selected dropdown values are the exact category item names from the gateway. */
public interface CategoryService {
    OperationResult<List<String>> getCategoryValues(String categoryName);
}
