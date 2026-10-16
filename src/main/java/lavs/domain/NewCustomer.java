package lavs.domain;

import java.time.LocalDate;

public record NewCustomer(
    String firstName,
    String familyName,
    LocalDate dateOfBirth,
    String nationality,
    long organisation,
    String title) {}
