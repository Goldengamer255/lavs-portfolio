package lavs.service;

/** Raw form values; parsing and business validation belong in the service layer. */
// AI-assisted: service contract scaffolding for team review.
public record AddressInput(String line1, String line2, String suburb, String city, String country, String postCode, String type, String otherType, boolean isMailing, boolean isPrimary) {}
