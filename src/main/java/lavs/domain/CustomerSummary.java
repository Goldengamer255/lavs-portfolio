package lavs.domain;

import java.time.LocalDate;

public record CustomerSummary(
    String number,
    String firstName,
    String familyName,
    LocalDate dateOfBirth,
    String title,
    String status
) {}