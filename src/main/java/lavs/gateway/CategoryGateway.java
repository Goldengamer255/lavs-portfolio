package lavs.gateway;

import java.util.List;

public interface CategoryGateway {
    OperationResult<List<String>> getCategoryValues(String categoryName);
}
