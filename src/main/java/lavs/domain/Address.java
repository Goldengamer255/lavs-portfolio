package lavs.domain;

/**
 * A customer's postal address, including its category and mailing/primary flags.
 * The identifier may be null before creation; {@code otherType} supplies the
 * custom description when the category is "Other".
 */
public record Address( 
    Long id,
    String customerNumber,
    String line1,
    String line2,
    String suburb,
    String city,
    String country,
    String postCode,
    String type,
    String otherType,
    boolean isMailing,
    boolean isPrimary
) {} 
