package lavs.domain;


/**
 * Customer phone contact with its category, primary flag, and texting preference.
 * The identifier may be null before creation, and the customer display name
 * may be absent in locally constructed or mutation-response records.
 */
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
