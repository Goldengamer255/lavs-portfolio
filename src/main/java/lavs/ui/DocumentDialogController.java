package lavs.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.domain.CustomerDocument;
import lavs.domain.DocumentMetadata;

/** Form for customer document metadata. No file contents are uploaded. */
public final class DocumentDialogController {
  private final CustomerDocument existing;

  @FXML private TextField fileNameField;
  @FXML private TextField fileTypeField;
  @FXML private TextField sizeField;
  @FXML private TextField typeField;
  @FXML private Label errorLabel;

  public DocumentDialogController(CustomerDocument existing) {
    this.existing = existing;
  }

  @FXML
  private void initialize() {
    if (existing != null) {
      fileNameField.setText(existing.fileName());
      fileTypeField.setText(existing.fileType());
      sizeField.setText(Long.toString(existing.size()));
      typeField.setText(existing.type());
    }
  }

  /** Returns null and shows a field message when metadata is incomplete. */
  public DocumentMetadata read() {
    String fileName = fileNameField.getText().trim();
    String fileType = fileTypeField.getText().trim();
    String type = typeField.getText().trim();
    if (fileName.isEmpty() || fileType.isEmpty() || type.isEmpty()) {
      showError("File name, file type, and document type are required.");
      return null;
    }
    try {
      long size = Long.parseLong(sizeField.getText().trim());
      if (size < 0) {
        showError("Size must be zero or greater.");
        return null;
      }
      errorLabel.setText("");
      return new DocumentMetadata(fileName, fileType, size, type);
    } catch (NumberFormatException e) {
      showError("Size must be a whole number of bytes.");
      return null;
    }
  }

  public void showError(String message) {
    errorLabel.setText(message);
  }
}
