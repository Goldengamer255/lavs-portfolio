package lavs.ui;

import java.util.List;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.domain.Phone;
import lavs.service.PhoneInput;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;
import lavs.ui.support.ValidationFeedback;
import lavs.validation.PhoneField;
import lavs.validation.FieldKey;

/**
 * Controller for phone dialog popup used to add or edit customer phone numbers.
 */
public final class PhoneDialogController {

    private final ValidationFeedback feedback = new ValidationFeedback();
    private final List<String> types;
    private final Phone existing;
    private final Function<PhoneInput, ValidationReport> validator;

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
    private TextField countryCodeField;
    @FXML
    private Label countryCodeError;
    @FXML
    private TextField numberField;
    @FXML
    private Label numberError;
    @FXML
    private CheckBox canTxtCheck;
    @FXML
    private Label canTxtError;

    /**
     * Creates the form controller.
     *
     * @param types     the phone types for the dropdown
     * @param existing  the phone number being edited, or null when adding one
     * @param validator checks the form without saving, e.g.
     *                  customers::validatePhone
     */
    public PhoneDialogController(List<String> types, Phone existing,
            Function<PhoneInput, ValidationReport> validator) {
        this.types = types;
        this.existing = existing;
        this.validator = validator;
    }

    @FXML
    private void initialize() {
        typeCombo.getItems().setAll(types);

        // Other type is only used when Type is "Other"
        otherTypeField.disableProperty().bind(typeCombo.valueProperty().isNotEqualTo("Other"));

        feedback.register(PhoneField.TYPE.key(), typeCombo, typeError);
        feedback.register(PhoneField.OTHER_TYPE.key(), otherTypeField, otherTypeError);
        feedback.register(PhoneField.COUNTRY_CODE.key(), countryCodeField, countryCodeError);
        feedback.register(PhoneField.NUMBER.key(), numberField, numberError);
        feedback.register(PhoneField.IS_PRIMARY.key(), primaryCheck, primaryError);
        feedback.register(PhoneField.CAN_TXT.key(), canTxtCheck, canTxtError);

        feedback.setFormErrorLabel(formErrorLabel);
        feedback.setOnFieldLeft(() -> feedback.showErrors(validator.apply(read())));

        if (existing != null) {
            typeCombo.setValue(existing.type());
            otherTypeField.setText(existing.otherType());
            countryCodeField.setText(existing.countryCode());
            numberField.setText(existing.number());
            primaryCheck.setSelected(existing.isPrimary());
            canTxtCheck.setSelected(existing.canTxt());
        }
    }

    /**
     * Builds and returns an PhoneInput record populated from the current form
     * values.
     * 
     * @return the input data gathered from the form fields
     */
    public PhoneInput read() {
        return new PhoneInput(
                countryCodeField.getText().trim(),
                numberField.getText().trim(),
                typeCombo.getValue(),
                otherTypeField.getText().trim(),
                primaryCheck.isSelected(),
                canTxtCheck.isSelected());
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
