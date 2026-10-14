package lavs.service;

/** Raw form values; parsing and business validation belong in the service layer. */
// AI-assisted: service contract scaffolding for team review.
public record EmployerInput(String name, String position, String salary, String type) {}
