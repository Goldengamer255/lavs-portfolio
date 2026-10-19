package lavs.domain;

import java.time.LocalDate;

/**
 * Customer identity and status returned by the mainframe.
 * The customer number identifies the saved record; an absent date of birth
 * is represented by null when mapped from the mainframe.
 */
public record Customer(
        String number,
        String firstName,
        String familyName,
        LocalDate dateOfBirth,
        String nationality,
        String title,
        String status
) {}