package lavs.domain;

public record Email(
    Long id,
    String customerName,
    String customerNumber,
    String address,
    boolean isPrimary,
    String number,
    String otherType,
    String type
) {}
