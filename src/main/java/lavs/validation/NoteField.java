package lavs.validation;

/**
 * Field keys for the Note form, matching the components of {@code NoteInput}.
 */
public enum NoteField implements FieldKey {
    TYPE("type"),
    LINES("lines");

    private final String key;

    NoteField(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
