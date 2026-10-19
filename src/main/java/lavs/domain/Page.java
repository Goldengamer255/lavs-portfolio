package lavs.domain;

import java.util.List;

/**
 * A page of results with navigation metadata from a gateway operation.
 * The item list is defensively copied into an unmodifiable list; the elements
 * themselves are not copied.
 *
 * @param <T> type of result item
 * @param items results contained in this page
 * @param page one-based page number
 * @param size requested page size, which may exceed the number of returned items
 * @param total total number of matching items across all pages
 */
public record Page<T>(
    List<T> items,
    long page,
    long size,
    long total
) {
    public Page {
        items = List.copyOf(items);
    }
}
