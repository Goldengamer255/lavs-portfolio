package lavs.service;

/** Service response data, independent of mainframe message classes. */
// AI-assisted: service contract scaffolding for team review.
public record Phone(long id, String customerNumber, String countryCode, String number, String type, String otherType, boolean isPrimary, boolean canTxt) {}
