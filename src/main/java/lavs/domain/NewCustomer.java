package lavs.domain;

import java.time.LocalDate;

/**
 * Input for creating a customer within an organisation.
 * The mainframe assigns the customer number and status, which are returned
 * in the resulting {@link Customer}.
 */
public record NewCustomer(
    String firstName,
    String familyName,
    LocalDate dateOfBirth,
    String nationality,
    long organisation,
    String title) {}
