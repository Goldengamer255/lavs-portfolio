package lavs.ui;

/**
 * Identifies whether a customer or loan screen is creating a record or working
 * with one that has already been saved.
 *
 * <p>Carried by {@link CustomerScreenContext} and {@link LoanScreenContext} to
 * guide form initialization and loading. This mode describes the record's
 * lifecycle, not whether the form is currently editable.</p>
 */
public enum ScreenMode {
    /** Initialize a form for a record that has not yet been saved. */
    NEW,

    /** Load a saved record using the identifier supplied in the screen context. */
    EXISTING
}
