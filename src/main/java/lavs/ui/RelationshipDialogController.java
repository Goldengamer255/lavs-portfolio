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

/** Controller for the add/edit customer relationship dialog. */
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

    public RelationshipDialogController(List<String> types, Relationship existing,
            Function<RelationshipInput, ValidationReport> validator) {
        this.types = List.copyOf(types);
        this.existing = existing;
        this.validator = validator;
    }

    @FXML
    private void initialize() {
        typeCombo.getItems().setAll(types);
        otherTypeField.disableProperty().bind(typeCombo.valueProperty().isNotEqualTo("Other"));

        feedback.register(RelationshipField.SECONDARY_CUSTOMER_NUMBER.key(),
                secondaryCustomerNumberField, secondaryCustomerNumberError);
        feedback.register(RelationshipField.TYPE.key(), typeCombo, typeError);
        feedback.register(RelationshipField.OTHER_TYPE.key(), otherTypeField, otherTypeError);
        feedback.setFormErrorLabel(formErrorLabel);
        feedback.setOnFieldLeft(() -> feedback.showErrors(validator.apply(read())));

        if (existing != null) {
            secondaryCustomerNumberField.setText(existing.secondaryCustomerNumber());
            typeCombo.setValue(existing.type());
            otherTypeField.setText(existing.otherType());
        }
    }

    public RelationshipInput read() {
        return new RelationshipInput(
                secondaryCustomerNumberField.getText().trim(),
                typeCombo.getValue(),
                otherTypeField.getText().trim());
    }

    public void showErrors(ValidationReport report) {
        feedback.showAllErrors(report);
    }

    public void showFormError(String message) {
        feedback.showAllErrors(new ValidationReport(
                List.of(new ValidationIssue(FieldKey.GLOBAL, message))));
    }
}
