package lavs.domain;

public record Address( 
    Long id,
    String customerNumber,
    String addressLine1,
    String addressLine2,
    String city,
    String Country,
    String postalCode,
    String type,
    String otherType,
    boolean isMailing,
    boolean isPrimary
) {

} 
