// src/main/java/lavs/domain/NewCustomer.java
package lavs.domain;

public record NewCustomer(
    String firstName,
    String familyName,
    String dateOfBirth,
    String nationality,
    long organisation,
    String title
) {}