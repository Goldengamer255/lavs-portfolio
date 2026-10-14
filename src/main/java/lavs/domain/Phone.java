package lavs.domain;


public record Phone(
    Long id,
    boolean canTxt,
    String countryCode,
    String customerName,
    String customerNumber,
    boolean isPrimary,
    String number,
    String otherType,
    String type
) {
}