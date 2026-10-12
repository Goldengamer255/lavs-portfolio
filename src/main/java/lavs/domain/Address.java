package lavs.domain;

public record Address( 
    Long id,
    String customerNumber,
    String line1,
    String line2,
    String suburb,
    String city,
    String country,
    String postCode,
    String type,
    String otherType,
    boolean isMailing,
    boolean isPrimary
) {

} 
