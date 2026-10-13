package lavs.service;

/** Raw form values; parsing and business validation belong in the service layer. */
// AI-assisted: service contract scaffolding for team review.
public record EmailInput(String address, String type, String otherType, boolean isPrimary) {}
