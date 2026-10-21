package lavs.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import lavs.domain.CustomerDocument;
import lavs.domain.DocumentMetadata;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;
import lavs.ui.support.ValidationFeedback;

/** Form for customer document metadata. No file contents are uploaded. */
public final class DocumentDialogController {
  private final CustomerDocument existing;
  private final ValidationFeedback feedback = new ValidationFeedback();

  @FXML private TextField fileNameField;
  @FXML private Label fileNameError;
  @FXML private TextField fileTypeField;
  @FXML private Label fileTypeError;
  @FXML private TextField sizeField;
  @FXML private Label sizeError;
  @FXML private TextField typeField;
  @FXML private Label typeError;
  @FXML private Label errorLabel;

  public DocumentDialogController(CustomerDocument existing) {
    this.existing = existing;
  }

  @FXML
  private void initialize() {
    feedback.register("fileName", fileNameField, fileNameError);
    feedback.register("fileType", fileTypeField, fileTypeError);
    feedback.register("size", sizeField, sizeError);
    feedback.register("type", typeField, typeError);
    feedback.setOnFieldLeft(() -> feedback.showErrors(validate()));

    // AI-assisted: adapted the Add Customer field-feedback pattern for document metadata.
    sizeField.setTextFormatter(new TextFormatter<>(change ->
        change.getControlNewText().matches("[0-9]*") ? change : null));

    if (existing != null) {
      fileNameField.setText(existing.fileName());
      fileTypeField.setText(existing.fileType());
      sizeField.setText(Long.toString(existing.size()));
      typeField.setText(existing.type());
    }
  }

  /** Returns null and marks invalid fields when metadata is incomplete. */
  public DocumentMetadata read() {
    ValidationReport report = validate();
    feedback.showAllErrors(report);
    if (!report.errors().isEmpty()) {
      return null;
    }
    return new DocumentMetadata(
        fileNameField.getText().trim(),
        fileTypeField.getText().trim(),
        Long.parseLong(sizeField.getText()),
        typeField.getText().trim());
  }

  private ValidationReport validate() {
    List<ValidationIssue> issues = new ArrayList<>();
    if (fileNameField.getText().isBlank()) {
      issues.add(new ValidationIssue("fileName", "File name is required."));
    }
    if (fileTypeField.getText().isBlank()) {
      issues.add(new ValidationIssue("fileType", "File type is required."));
    }
    String sizeText = sizeField.getText();
    if (sizeText.isBlank()) {
      issues.add(new ValidationIssue("size", "Enter the size in bytes."));
    } else {
      try {
        Long.parseLong(sizeText);
      } catch (NumberFormatException e) {
        issues.add(new ValidationIssue("size", "Size is too large."));
      }
    }
    if (typeField.getText().isBlank()) {
      issues.add(new ValidationIssue("type", "Document type is required."));
    }
    return new ValidationReport(issues);
  }

  public void showError(String message) {
    errorLabel.setText(message);
    errorLabel.setVisible(true);
    errorLabel.setManaged(true);
  }
}
