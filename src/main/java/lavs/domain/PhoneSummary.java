package lavs.domain;

public record PhoneSummary(
    Long id,
    String countryCode,
    String number,
    String type,
    boolean canTxt,
    boolean isPrimary
) {}