package lavs.domain;

/** Service response data, independent of mainframe message classes. */
// AI-assisted: service contract scaffolding for team review.
public record Email(long id, String customerNumber, String address, String type, String otherType, boolean isPrimary) {}
