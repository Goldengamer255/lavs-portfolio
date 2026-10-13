package lavs.service;

/** Raw form values. The service obtains organisation from application configuration. */
// AI-assisted: service contract scaffolding for team review.
public record CustomerInput(String firstName, String familyName, java.time.LocalDate dateOfBirth, String nationality, String title) {}
