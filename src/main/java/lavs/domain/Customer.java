package lavs.domain;

import java.time.LocalDate;

public record Customer(
        String number,
        String firstName,
        String familyName,
        LocalDate dateOfBirth,
        String nationality,
        String title,
        String status
) {}