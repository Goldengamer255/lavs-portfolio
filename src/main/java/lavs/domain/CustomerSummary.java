package lavs.domain;

import java.time.LocalDate;

/**
 * Customer search result containing identification and display details.
 * The customer number can be used to retrieve the full {@link Customer} record.
 */
public record CustomerSummary(
    String number,
    String firstName,
    String familyName,
    LocalDate dateOfBirth,
    String title,
    String status
) {}