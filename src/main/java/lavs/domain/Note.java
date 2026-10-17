package lavs.domain;

import java.time.LocalDate;

/**
 * A customer note. The mainframe sets the author and the date added; lines
 * holds note text with a newline between each line.
 */
public record Note(
        long id,
        String customerNumber,
        String type,
        String lines,
        String author,
        LocalDate dateAdded) {
}
