package lavs.validation;

/** Field keys for the Phone form, matching the components of {@code PhoneInput}. */
public enum PhoneField implements FieldKey {
    COUNTRY_CODE("countryCode"),
    NUMBER("number"),
    TYPE("type"),
    OTHER_TYPE("otherType"),
    IS_PRIMARY("isPrimary"),
    CAN_TXT("canTxt");

    private final String key;

    PhoneField(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
