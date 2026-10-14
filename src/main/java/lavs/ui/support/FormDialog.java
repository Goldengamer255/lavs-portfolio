package lavs.ui.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;

// AI-assisted: written by me from Claude's outline; the Save button event filter came from Claude
/**
 * Helper to pop open standalone FXML forms with a save/cancel workflow.
 * Keeps the dialog alive during async/validation so users don't lose input.
 */
public final class FormDialog {

    private FormDialog() {
    }

    public static void show(Window owner, String title, String fxml, Object controller,
            ObservableBooleanValue busy, Consumer<Runnable> onSave) {

        FXMLLoader loader = new FXMLLoader(FormDialog.class.getResource(fxml));
        loader.setController(controller);

        Parent content;
        try {
            content = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed loading FXML: " + fxml, e);
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        if (owner.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveButton.disableProperty().bind(busy);
        content.disableProperty().bind(busy);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).disableProperty().bind(busy);
        dialog.setOnCloseRequest(event -> {
            if (busy.get()) {
                event.consume();
            }
        });

        // Intercept action to handle validation/save without auto-closing the popup
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            onSave.accept(dialog::close);
        });

        dialog.show();
    }
}