package lavs.ui;

import java.util.List;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.domain.Employer;
import lavs.service.EmployerInput;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;
import lavs.ui.support.ValidationFeedback;
import lavs.validation.EmployerField;
import lavs.validation.FieldKey;

/**
 * Controller for employer dialog popup used to add or edit customer employers.
 */
public final class EmployerDialogController {

    private final ValidationFeedback feedback = new ValidationFeedback();
    private final List<String> types;
    private final Employer existing;
    private final Function<EmployerInput, ValidationReport> validator;

    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private Label typeError;
    @FXML
    private Label formErrorLabel;
    @FXML
    private TextField nameField;
    @FXML
    private Label nameError;
    @FXML
    private TextField positionField;
    @FXML
    private Label positionError;
    @FXML
    private TextField salaryField;
    @FXML
    private Label salaryError;

    /**
     * Creates the form controller.
     *
     * @param types     the employer types for the dropdown
     * @param existing  the employer being edited, or null when adding one
     * @param validator checks the form without saving, e.g.
     *                  customers::validateEmployer
     */
    public EmployerDialogController(List<String> types, Employer existing,
            Function<EmployerInput, ValidationReport> validator) {
        this.types = types;
        this.existing = existing;
        this.validator = validator;
    }

    @FXML
    private void initialize() {
        typeCombo.getItems().setAll(types);

        feedback.register(EmployerField.TYPE.key(), typeCombo, typeError);
        feedback.register(EmployerField.NAME.key(), nameField, nameError);
        feedback.register(EmployerField.POSITION.key(), positionField, positionError);
        feedback.register(EmployerField.SALARY.key(), salaryField, salaryError);

        feedback.setFormErrorLabel(formErrorLabel);
        feedback.setOnFieldLeft(() -> feedback.showErrors(validator.apply(read())));

        if (existing != null) {
            typeCombo.setValue(existing.type());
            nameField.setText(existing.name());
            positionField.setText(existing.position());
            salaryField.setText(String.valueOf(existing.salary()));
        }
    }

    /**
     * Builds and returns an EmployerInput record populated from the current form
     * values.
     *
     * @return the input data gathered from the form fields
     */
    public EmployerInput read() {
        return new EmployerInput(
                nameField.getText().trim(),
                positionField.getText().trim(),
                salaryField.getText().trim(),
                typeCombo.getValue());
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
