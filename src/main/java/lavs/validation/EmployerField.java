package lavs.validation;

public enum EmployerField implements FieldKey {
    NAME("name"),
    POSITION("position"),
    SALARY("salary"),
    TYPE("type");

    private String key;

    EmployerField(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
