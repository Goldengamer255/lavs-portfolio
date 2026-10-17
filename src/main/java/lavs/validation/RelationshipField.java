package lavs.validation;

/** Field keys for the customer relationship form. */
public enum RelationshipField implements FieldKey {
    SECONDARY_CUSTOMER_NUMBER("secondaryCustomerNumber"),
    TYPE("type"),
    OTHER_TYPE("otherType");

    private final String key;

    RelationshipField(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
