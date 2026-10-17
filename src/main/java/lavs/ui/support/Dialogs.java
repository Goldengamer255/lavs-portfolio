package lavs.ui.support;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

// AI-assisted: Helper methods provided by Claude to standardise popups.
/**
 * Small confirmation and message popups shared by the screens.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * Asks whether to throw away unsaved changes.
     *
     * @return true if the user chose Discard
     */
    public static boolean confirmDiscard(Window owner) {
        ButtonType discard = new ButtonType("Discard", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. Discard them and leave this screen?", discard, ButtonType.CANCEL);
        prepare(alert, owner, "Unsaved changes");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == discard;
    }

    /**
     * Confirms logout because leaving the authenticated session discards any
     * unsaved changes on the current screen.
     *
     * @return true if the user chose Log Out
     */
    public static boolean confirmLogout(Window owner) {
        ButtonType logout = new ButtonType("Log Out", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Log out? Any unsaved changes will be lost.", logout, ButtonType.CANCEL);
        prepare(alert, owner, "Log Out");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == logout;
    }

    /**
     * Shows a short message with an OK button.
     */
    public static void showInfo(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        prepare(alert, owner, title);
        alert.showAndWait();
    }

    private static void prepare(Alert alert, Window owner, String title) {
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        if (owner.getScene() != null) {
            alert.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
    }
}
