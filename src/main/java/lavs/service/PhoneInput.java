package lavs.service;

/** Raw form values; parsing and business validation belong in the service layer. */
// AI-assisted: service contract scaffolding for team review.
public record PhoneInput(String countryCode, String number, String type, String otherType, boolean isPrimary, boolean canTxt) {}
