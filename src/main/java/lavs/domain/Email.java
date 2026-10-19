package lavs.domain;

/**
 * Customer email contact with its category and primary-contact flag.
 * The identifier may be null before creation, and the customer display name
 * may be absent in locally constructed or mutation-response records.
 */
public record Email(
    Long id,
    String customerName,
    String customerNumber,
    String address,
    boolean isPrimary,
    String otherType,
    String type
) {}
