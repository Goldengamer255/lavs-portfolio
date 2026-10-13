package lavs.validation;

public enum LoanField implements FieldKey{
    PRINCIPAL("principal"),
    PAYMENT_AMOUNT("paymentAmount"),
    RATE_TYPE("rateType"),
    RATE_AMOUNT("rateAmount"),
    RATE_FREQUENCY("rateFrequency"),
    PAYMENT_FREQUENCY("paymentFrequency"),
    START_DATE("startDate"),
    RENEWAL_DATE("renewalDate");

    private final String key;

    LoanField(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
