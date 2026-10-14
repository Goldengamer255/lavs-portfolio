package lavs.domain;

public record AddressSummary(
    Long id,
    String city,
    String country,
    String type,
    boolean isMailing,
    boolean isPrimary
) {}
