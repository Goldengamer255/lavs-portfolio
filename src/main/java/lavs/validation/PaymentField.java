package lavs.validation;

/**
 * Field keys for the Payment form, matching the components of {@code PaymentInput}.
 */
public enum PaymentField implements FieldKey {
    AMOUNT("amount"),
    DATE("date");

    private final String key;

    PaymentField(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
