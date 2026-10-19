package lavs.domain;

/**
 * Email list entry containing the address, category, and primary-contact flag.
 * Retrieve the full {@link Email} for customer details and any custom type description.
 */
public record EmailSummary(
    Long id,
    String address,
    boolean isPrimary,
    String type
) {}
