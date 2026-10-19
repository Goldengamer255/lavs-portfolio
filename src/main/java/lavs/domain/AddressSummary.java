package lavs.domain;

/**
 * Address list entry containing location, category, and mailing/primary flags.
 * Retrieve the full {@link Address} for street details before editing.
 */
public record AddressSummary(
    Long id,
    String city,
    String country,
    String type,
    boolean isMailing,
    boolean isPrimary
) {}
