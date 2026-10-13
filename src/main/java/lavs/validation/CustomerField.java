package lavs.validation;

// AI-assisted: enum pattern structure.
public enum CustomerField implements FieldKey {
    FIRST_NAME("firstName"),
    FAMILY_NAME("familyName"),
    DATE_OF_BIRTH("dateOfBirth"),
    NATIONALITY("nationality"),
    TITLE("title");

    private final String key;

    CustomerField(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
