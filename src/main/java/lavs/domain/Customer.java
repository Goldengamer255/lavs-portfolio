package lavs.domain;

public record Customer(
        String number,
        String firstName,
        String familyName,
        String dateOfBirth,
        String nationality,
        String title,
        String status
) {}