package lavs.domain;


public record Phone(
    boolean canTxt,
    String countryCode,
    String customerName,
    String customerNumber,
    Long id,
    boolean isPrimary,
    String number,
    String otherType,
    String type
) {}