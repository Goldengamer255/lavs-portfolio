package lavs.domain;

public record EmailSummary(
    Long _id,
    String _address,
    String isPrimary,
    String _type
) {}
