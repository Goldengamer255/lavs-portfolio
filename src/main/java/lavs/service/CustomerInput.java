package lavs.service;

/** Raw form values; parsing and business validation belong in the service layer. */
// AI-assisted: service contract scaffolding for team review.
public record CustomerInput(String firstName, String familyName, java.time.LocalDate dateOfBirth, String nationality, String title, String organisation) {}
