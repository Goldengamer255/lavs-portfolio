package lavs.domain;

/** Summary returned when listing a customer's relationships. */
public record RelationshipSummary(Long id, String secondaryCustomerNumber, String type) {
}
