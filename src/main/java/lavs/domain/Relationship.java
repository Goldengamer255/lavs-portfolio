package lavs.domain;

/** A relationship from one customer to another customer. */
public record Relationship(
        Long id,
        String customerNumber,
        String customerName,
        String secondaryCustomerNumber,
        String secondaryCustomerName,
        String type,
        String otherType) {
}
