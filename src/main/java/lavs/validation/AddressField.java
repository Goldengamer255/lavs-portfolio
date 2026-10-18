package lavs.validation;

/** Field keys for the Address form, matching the components of {@code AddressInput}. */
public enum AddressField implements FieldKey {
    LINE1("line1"),
    LINE2("line2"),
    SUBURB("suburb"),
    CITY("city"),
    COUNTRY("country"),
    POST_CODE("postCode"),
    OTHER_TYPE("otherType"),
    IS_MAILING("isMailing"),
    IS_PRIMARY("isPrimary"),
    TYPE("type");

    private final String key;

    AddressField(final String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
