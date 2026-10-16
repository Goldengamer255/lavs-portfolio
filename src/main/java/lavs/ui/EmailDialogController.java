package lavs.ui;

import java.util.List;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.domain.Email;
import lavs.service.EmailInput;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;
import lavs.ui.support.ValidationFeedback;
import lavs.validation.EmailField;
import lavs.validation.FieldKey;

/**
 * Controller for email dialog popup to add or edit customer email addresses.
 */
public final class EmailDialogController {

    private final ValidationFeedback feedback = new ValidationFeedback();
    private final List<String> types;
    private final Email existing;
    private final Function<EmailInput, ValidationReport> validator;

    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private Label typeError;
    @FXML
    private TextField otherTypeField;
    @FXML
    private Label otherTypeError;
    @FXML
    private CheckBox primaryCheck;
    @FXML
    private Label primaryError;
    @FXML
    private Label formErrorLabel;
    @FXML
    private TextField addressField;
    @FXML
    private Label addressError;

    /**
     * Creates the form controller.
     *
     * @param types     the email types for the dropdown
     * @param existing  the email address being edited, or null when adding one
     * @param validator checks the form without saving, e.g.
     *                  customers::validateEmail
     */
    public EmailDialogController(List<String> types, Email existing,
            Function<EmailInput, ValidationReport> validator) {
        this.types = types;
        this.existing = existing;
        this.validator = validator;
    }

    @FXML
    private void initialize() {
        typeCombo.getItems().setAll(types);

        // Other type is only used when Type is "Other"
        otherTypeField.disableProperty().bind(typeCombo.valueProperty().isNotEqualTo("Other"));

        feedback.register(EmailField.TYPE.key(), typeCombo, typeError);
        feedback.register(EmailField.OTHER_TYPE.key(), otherTypeField, otherTypeError);
        feedback.register(EmailField.ADDRESS.key(), addressField, addressError);
        feedback.register(EmailField.IS_PRIMARY.key(), primaryCheck, primaryError);

        feedback.setFormErrorLabel(formErrorLabel);
        feedback.setOnFieldLeft(() -> feedback.showErrors(validator.apply(read())));

        if (existing != null) {
            typeCombo.setValue(existing.type());
            otherTypeField.setText(existing.otherType());
            addressField.setText(existing.address());
            primaryCheck.setSelected(existing.isPrimary());
        }
    }

    /**
     * Builds and returns an EmailInput record populated from the current form
     * values.
     *
     * @return the input data gathered from the form fields
     */
    public EmailInput read() {
        return new EmailInput(
                addressField.getText().trim(),
                typeCombo.getValue(),
                otherTypeField.getText().trim(),
                primaryCheck.isSelected());
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
