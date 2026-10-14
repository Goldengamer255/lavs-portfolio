package lavs.validation;

/** Field keys for the Employer form, matching the components of {@code EmployerInput}. */
public enum EmployerField implements FieldKey {
    NAME("name"),
    POSITION("position"),
    SALARY("salary"),
    TYPE("type");

    private final String key;

    EmployerField(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
