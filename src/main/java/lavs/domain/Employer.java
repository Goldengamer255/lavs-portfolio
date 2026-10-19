package lavs.domain;

/**
 * Employment details associated with a customer, including position and salary.
 * The identifier may be null before creation, and the customer display name
 * may be absent in locally constructed or mutation-response records.
 */
public record Employer(
    Long id,
    String customerName,
    String customerNumber,
    String name,
    String position,
    double salary,
    String type
) {}
