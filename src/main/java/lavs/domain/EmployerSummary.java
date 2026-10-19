package lavs.domain;

/**
 * Employment list entry containing the employer name and category.
 * Retrieve the full {@link Employer} for position and salary details before editing.
 */
public record EmployerSummary(
    Long id,
    String name,
    String type
) {}
