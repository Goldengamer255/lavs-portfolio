package lavs.domain;

/**
 * Phone list entry containing dialling details, category, and contact flags.
 * Retrieve the full {@link Phone} for customer details and any custom type description.
 */
public record PhoneSummary(
    Long id,
    String countryCode,
    String number,
    String type,
    boolean canTxt,
    boolean isPrimary
) {}