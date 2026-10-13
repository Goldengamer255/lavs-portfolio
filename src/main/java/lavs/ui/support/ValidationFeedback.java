package lavs.ui.support;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javafx.css.PseudoClass;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;

// AI-assisted: class layout plus the focus listener, apply and showAllErrors from Claude; all other methods written by me
/**
 * Shows validation errors from the service layer on form fields they belong to:
 * a red outline on the field and the message in the field's error label.
 * While user is typing, errors only appear on fields they have already left,
 * so a new blank form doesn't start covered in red. Saving shows every error.
 */
public final class ValidationFeedback {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private final Map<String, Control> fields = new LinkedHashMap<>();
    private final Map<String, Label> errorLabels = new LinkedHashMap<>();
    private final Set<String> touched = new HashSet<>();
    private Label formErrorLabel;
    private Runnable onFieldLeft = () -> {
    };
    private boolean submitted;

    /**
     * Connects a field key from the service, e.g. "firstName", to its control and
     * error label.
     */
    public void register(String fieldKey, Control field, Label errorLabel) {
        fields.put(fieldKey, field);
        errorLabels.put(fieldKey, errorLabel);
        field.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused) {
                touched.add(fieldKey);
                onFieldLeft.run();
            }
        });
    }

    /**
     * Sets the label for errors that don't belong to a single field.
     */
    public void setFormErrorLabel(Label label) {
        this.formErrorLabel = label;
    }

    /**
     * Sets what runs when the user leaves a field, usually re-validating the form.
     */
    public void setOnFieldLeft(Runnable action) {
        this.onFieldLeft = action != null ? action : () -> {
        };
    }

    /**
     * Shows errors only on fields the user has already left. Use while they're
     * filling in the form.
     */
    public void showErrors(ValidationReport report) {
        apply(report);
    }

    /**
     * Shows every error and moves focus to the first field with one. Use when they
     * click Save.
     */
    public void showAllErrors(ValidationReport report) {
        submitted = true;
        touched.addAll(fields.keySet());
        apply(report);
        for (Control field : fields.values()) {
            if (field.getPseudoClassStates().contains(ERROR)) {
                field.requestFocus();
                return;
            }
        }
    }

    /**
     * Removes every error and resets the form to untouched, e.g. after Cancel or a
     * successful save.
     */
    public void clear() {
        submitted = false;
        touched.clear();
        for (String key : fields.keySet()) {
            clearFieldError(key);
        }
        showFormError(null);
    }

    // Matches error to field. Errors with no matching field (including "_global")
    // go to the form error label once the user has tried to save, so none get lost.
    private void apply(ValidationReport report) {
        Map<String, String> messages = new LinkedHashMap<>();
        for (ValidationIssue issue : report.errors()) {
            messages.merge(issue.fieldKey(), issue.message(), (first, second) -> first + "\n" + second);
        }
        for (String key : fields.keySet()) {
            String message = messages.remove(key);
            if (message != null && touched.contains(key)) {
                setFieldError(key, message);
            } else {
                clearFieldError(key);
            }
        }
        if (submitted && !messages.isEmpty()) {
            showFormError(String.join("\n", messages.values()));
        } else {
            showFormError(null);
        }
    }

    private void setFieldError(String key, String message) {
        Control field = fields.get(key);
        Label label = errorLabels.get(key);

        if (field != null) {
            field.pseudoClassStateChanged(ERROR, true);
        }
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
        }
    }

    private void clearFieldError(String key) {
        Control field = fields.get(key);
        Label label = errorLabels.get(key);

        if (field != null) {
            field.pseudoClassStateChanged(ERROR, false);
        }
        if (label != null) {
            label.setText("");
            label.setVisible(false);
        }
    }

    private void showFormError(String message) {
        if (formErrorLabel == null) {
            return;
        }

        if (message == null || message.isEmpty()) {
            formErrorLabel.setText("");
            formErrorLabel.setVisible(false);
            formErrorLabel.setManaged(false);
        } else {
            formErrorLabel.setText(message);
            formErrorLabel.setVisible(true);
            formErrorLabel.setManaged(true);
        }
    }
}