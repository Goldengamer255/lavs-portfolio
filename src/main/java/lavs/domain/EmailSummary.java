package lavs.domain;

public record EmailSummary(
    Long id,
    String address,
    boolean isPrimary,
    String type
) {}
