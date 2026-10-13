package lavs.validation;

public enum EmailField implements FieldKey {
    ADDRESS("address"),
    TYPE("type"),
    OTHER_TYPE("otherType"),
    IS_PRIMARY("isPrimary");

    private final String key;

    EmailField(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
