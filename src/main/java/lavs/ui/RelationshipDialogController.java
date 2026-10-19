package lavs.ui;

import java.util.List;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.domain.Relationship;
import lavs.service.RelationshipInput;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;
import lavs.ui.support.ValidationFeedback;
import lavs.validation.FieldKey;
import lavs.validation.RelationshipField;

/**
 * Manages form values and validation feedback for adding or editing a customer
 * relationship.
 *
 * <p>Validation is supplied by the caller, while persistence is coordinated by
 * {@link RelationshipsTab}. Methods accessing form controls must run on the
 * JavaFX application thread after FXML initialization.</p>
 */
public final class RelationshipDialogController {

    private final ValidationFeedback feedback = new ValidationFeedback();
    private final List<String> types;
    private final Relationship existing;
    private final Function<RelationshipInput, ValidationReport> validator;

    @FXML
    private TextField secondaryCustomerNumberField;
    @FXML
    private Label secondaryCustomerNumberError;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private Label typeError;
    @FXML
    private TextField otherTypeField;
    @FXML
    private Label otherTypeError;
    @FXML
    private Label formErrorLabel;

    /**
     * Supplies the dialog data before the FXML loader injects its controls.
     *
     * @param types available relationship types, copied to isolate dialog options
     * @param existing relationship to edit, or {@code null} for a new relationship
     * @param validator synchronous validation function invoked when a field loses focus
     */
    public RelationshipDialogController(List<String> types, Relationship existing,
            Function<RelationshipInput, ValidationReport> validator) {
        this.types = List.copyOf(types);
        this.existing = existing;
        this.validator = validator;
    }

    @FXML
    private void initialize() {
        typeCombo.getItems().setAll(types);
        // A custom description is editable only for "Other"; disabling the field
        // preserves its contents if the user switches between relationship types.
        otherTypeField.disableProperty().bind(typeCombo.valueProperty().isNotEqualTo("Other"));

        feedback.register(RelationshipField.SECONDARY_CUSTOMER_NUMBER.key(),
                secondaryCustomerNumberField, secondaryCustomerNumberError);
        feedback.register(RelationshipField.TYPE.key(), typeCombo, typeError);
        feedback.register(RelationshipField.OTHER_TYPE.key(), otherTypeField, otherTypeError);
        feedback.setFormErrorLabel(formErrorLabel);
        // Validate the full input on focus loss, while feedback limits initial
        // error display to fields the user has already left.
        feedback.setOnFieldLeft(() -> feedback.showErrors(validator.apply(read())));

        if (existing != null) {
            secondaryCustomerNumberField.setText(existing.secondaryCustomerNumber());
            typeCombo.setValue(existing.type());
            otherTypeField.setText(existing.otherType());
        }
    }

    /**
     * Captures the current input, trimming surrounding whitespace from text fields.
     * This method does not validate or persist the values.
     *
     * @return input containing the customer number, selected type, and custom description
     */
    public RelationshipInput read() {
        return new RelationshipInput(
                secondaryCustomerNumberField.getText().trim(),
                typeCombo.getValue(),
                otherTypeField.getText().trim());
    }

    /**
     * Displays all reported validation errors and focuses the first invalid field.
     *
     * @param report validation result returned by the save workflow
     */
    public void showErrors(ValidationReport report) {
        feedback.showAllErrors(report);
    }

    /**
     * Displays an operation failure that is not associated with a specific field.
     *
     * @param message user-facing error to display at form level
     */
    public void showFormError(String message) {
        feedback.showAllErrors(new ValidationReport(
                List.of(new ValidationIssue(FieldKey.GLOBAL, message))));
    }
}
