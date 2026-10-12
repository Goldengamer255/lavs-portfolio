package lavs.domain;

public record CustomerSummary(
    String number,
    String firstName,
    String familyName,
    String dateOfBirth,
    String title,
    String status
) {}