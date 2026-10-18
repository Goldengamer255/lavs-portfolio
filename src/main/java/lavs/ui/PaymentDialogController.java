package lavs.ui;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.service.PaymentInput;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;
import lavs.ui.support.ValidationFeedback;
import lavs.validation.FieldKey;
import lavs.validation.PaymentField;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Controller for the popup used to add a payment to a loan.
 */
// AI-assisted: adapted from NoteDialogController with AI assistance; payment fields and save logic written by me
public final class PaymentDialogController {

    private final ValidationFeedback feedback = new ValidationFeedback();
    private final Function<PaymentInput, ValidationReport> validator;

    @FXML private TextField amountField;
    @FXML private Label amountError;
    @FXML private DatePicker dateField;
    @FXML private Label dateError;
    @FXML private Label formErrorLabel;

    /**
     * Creates the form controller.
     *
     * @param validator checks the form without saving, e.g. payments::validatePayment
     */
    public PaymentDialogController(Function<PaymentInput, ValidationReport> validator) {
        this.validator = validator;
    }

    /** Registers the fields for validation messages and defaults the date to today. */
    @FXML
    private void initialize() {
        feedback.register(PaymentField.AMOUNT.key(), amountField, amountError);
        feedback.register(PaymentField.DATE.key(), dateField, dateError);
        feedback.setFormErrorLabel(formErrorLabel);
        feedback.setOnFieldLeft(() -> feedback.showErrors(validator.apply(read())));
        dateField.setValue(LocalDate.now());
        dateField.setEditable(false);
    }

    /**
     * Builds a PaymentInput from the current form values.
     *
     * @return the amount text and chosen date from the form
     */
    public PaymentInput read() {
        return new PaymentInput(amountField.getText(), dateField.getValue());
    }

    /**
     * Displays validation error messages on the form fields based on the given report.
     *
     * @param report the validation report containing errors to display
     */
    public void showErrors(ValidationReport report) { feedback.showAllErrors(report); }

    /** Displays a global error message not tied to a specific field. */
    public void showFormError(String message) {
        feedback.showAllErrors(new ValidationReport(List.of(new ValidationIssue(FieldKey.GLOBAL, message))));
    }
}
