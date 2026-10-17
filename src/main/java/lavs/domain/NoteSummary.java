package lavs.domain;

import java.time.LocalDate;

/**
 * A row in a customer's note list. The list doesn't include the note text, so
 * editing a note retrieves the full {@link Note} first.
 */
public record NoteSummary(
        long id,
        String type,
        String author,
        LocalDate dateAdded,
        long numberOfLines) {
}