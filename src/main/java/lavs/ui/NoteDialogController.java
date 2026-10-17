package lavs.ui;

import java.util.List;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import lavs.domain.Note;
import lavs.service.NoteInput;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;
import lavs.ui.support.ValidationFeedback;
import lavs.validation.NoteField;
import lavs.validation.FieldKey;

/**
 * Controller for the note popup used to add or edit a customer note.
 */
public final class NoteDialogController {

    private final ValidationFeedback feedback = new ValidationFeedback();
    private final Note existing;
    private final Function<NoteInput, ValidationReport> validator;

    @FXML
    private TextField typeField;
    @FXML
    private Label typeError;
    @FXML
    private TextArea linesArea;
    @FXML
    private Label linesError;
    @FXML
    private Label formErrorLabel;

    /**
     * Creates the form controller.
     *
     * @param existing  the note being edited, or null when adding one
     * @param validator checks the form without saving, e.g. notes::validateNote
     */
    public NoteDialogController(Note existing,
            Function<NoteInput, ValidationReport> validator) {
        this.existing = existing;
        this.validator = validator;
    }

    @FXML
    private void initialize() {
        feedback.register(NoteField.TYPE.key(), typeField, typeError);
        feedback.register(NoteField.LINES.key(), linesArea, linesError);

        feedback.setFormErrorLabel(formErrorLabel);
        feedback.setOnFieldLeft(() -> feedback.showErrors(validator.apply(read())));

        if (existing != null) {
            typeField.setText(existing.type());
            linesArea.setText(existing.lines());
        }
    }

    /**
     * Builds and returns a NoteInput record populated from the current form
     * values.
     *
     * @return the input data gathered from the form fields
     */
    public NoteInput read() {
        return new NoteInput(typeField.getText(), linesArea.getText());
    }

    /**
     * Displays validation error messages on the form fields based on the given
     * report.
     *
     * @param report the validation report containing errors to display
     */
    public void showErrors(ValidationReport report) {
        feedback.showAllErrors(report);
    }

    /** Displays a global error message not tied to a specific field. */
    public void showFormError(String message) {
        feedback.showAllErrors(new ValidationReport(List.of(new ValidationIssue(FieldKey.GLOBAL, message))));
    }
}
