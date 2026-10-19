package lavs.gateway;

import java.util.List;

/** Supplies mainframe category values used as selectable options in application forms. */
public interface CategoryGateway {
    /** Retrieves the values for the named mainframe category. */
    OperationResult<List<String>> getCategoryValues(String categoryName);
}
