package lavs.domain;

import java.util.List;

public record Page<T>(
    List<T> items,
    long page,
    long size,
    long total
) {
    public page{
        items = List.copyOf(items);
    }
}
