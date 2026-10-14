package lavs.domain;

public record Employer(
    Long id,
    String customerName,
    String customerNumber,
    String name,
    String position,
    double salary,
    String type
) {}
