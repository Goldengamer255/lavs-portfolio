package lavs.service;

/** Raw values entered in the customer relationship form. */
public record RelationshipInput(
        String secondaryCustomerNumber,
        String type,
        String otherType) {
}
