package lavs.ui;

import java.util.List;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.domain.Address;
import lavs.service.AddressInput;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;
import lavs.ui.support.ValidationFeedback;
import lavs.validation.AddressField;
import lavs.validation.FieldKey;

// AI-assisted: mostly written by me; the "Other type" binding came from Claude
/**
 * Controller for address dialog popup used to add or edit customer addresses.
 */
public final class AddressDialogController {

    private final ValidationFeedback feedback = new ValidationFeedback();
    private final List<String> types;
    private final Address existing;
    private final Function<AddressInput, ValidationReport> validator;

    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private Label typeError;
    @FXML
    private TextField otherTypeField;
    @FXML
    private Label otherTypeError;
    @FXML
    private TextField line1Field;
    @FXML
    private Label line1Error;
    @FXML
    private TextField line2Field;
    @FXML
    private Label line2Error;
    @FXML
    private TextField suburbField;
    @FXML
    private Label suburbError;
    @FXML
    private TextField cityField;
    @FXML
    private Label cityError;
    @FXML
    private TextField postCodeField;
    @FXML
    private Label postCodeError;
    @FXML
    private TextField countryField;
    @FXML
    private Label countryError;
    @FXML
    private CheckBox primaryCheck;
    @FXML
    private Label primaryError;
    @FXML
    private CheckBox mailingCheck;
    @FXML
    private Label mailingError;
    @FXML
    private Label formErrorLabel;

    /**
     * Creates the form controller.
     *
     * @param types     the address types for the dropdown
     * @param existing  the address being edited, or null when adding one
     * @param validator checks the form without saving, e.g.
     *                  customers::validateAddress
     */
    public AddressDialogController(List<String> types, Address existing,
            Function<AddressInput, ValidationReport> validator) {
        this.types = types;
        this.existing = existing;
        this.validator = validator;
    }

    @FXML
    private void initialize() {
        typeCombo.getItems().setAll(types);

        // Other type is only used when Type is "Other"
        otherTypeField.disableProperty().bind(typeCombo.valueProperty().isNotEqualTo("Other"));

        feedback.register(AddressField.TYPE.key(), typeCombo, typeError);
        feedback.register(AddressField.OTHER_TYPE.key(), otherTypeField, otherTypeError);
        feedback.register(AddressField.LINE1.key(), line1Field, line1Error);
        feedback.register(AddressField.LINE2.key(), line2Field, line2Error);
        feedback.register(AddressField.SUBURB.key(), suburbField, suburbError);
        feedback.register(AddressField.CITY.key(), cityField, cityError);
        feedback.register(AddressField.POST_CODE.key(), postCodeField, postCodeError);
        feedback.register(AddressField.COUNTRY.key(), countryField, countryError);
        feedback.register(AddressField.IS_PRIMARY.key(), primaryCheck, primaryError);
        feedback.register(AddressField.IS_MAILING.key(), mailingCheck, mailingError);

        feedback.setFormErrorLabel(formErrorLabel);
        feedback.setOnFieldLeft(() -> feedback.showErrors(validator.apply(read())));

        if (existing != null) {
            typeCombo.setValue(existing.type());
            otherTypeField.setText(existing.otherType());
            line1Field.setText(existing.line1());
            line2Field.setText(existing.line2());
            suburbField.setText(existing.suburb());
            cityField.setText(existing.city());
            postCodeField.setText(existing.postCode());
            countryField.setText(existing.country());
            primaryCheck.setSelected(existing.isPrimary());
            mailingCheck.setSelected(existing.isMailing());
        }
    }

    /**
     * Builds and returns an AddressInput record populated from the current form
     * values.
     *
     * @return the input data gathered from the form fields
     */
    public AddressInput read() {
        return new AddressInput(
                line1Field.getText().trim(),
                line2Field.getText().trim(),
                suburbField.getText().trim(),
                cityField.getText().trim(),
                countryField.getText().trim(),
                postCodeField.getText().trim(),
                typeCombo.getValue(),
                otherTypeField.getText().trim(),
                mailingCheck.isSelected(),
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
