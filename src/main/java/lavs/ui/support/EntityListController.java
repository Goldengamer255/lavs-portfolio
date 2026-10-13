package lavs.ui.support;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

// AI-assisted: class design and code structure suggested by Claude, typed and checked by me
/**
 * Controller for EntityListView.fxml, a reusable table with Add, Edit and
 * Delete buttons.
 * The screen that includes it chooses the columns and each button's function.
 *
 * @param <T> the type of item shown in each row, e.g. an address
 */
public final class EntityListController<T> {

    private static final String HINT = "Select a row to edit or delete it.";
    private static final String HINT_NO_EDIT = "Select a row to delete it.";
    private static final String ERROR_STYLE = "field-error";

    @FXML
    private Button addButton;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private ProgressIndicator listSpinner;
    @FXML
    private TableView<T> table;
    @FXML
    private Label messageLabel;

    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private String hint = HINT;
    private Runnable onAdd = () -> {
    };
    private Consumer<T> onEdit = item -> {
    };
    private Consumer<T> onDelete = item -> {
    };

    @FXML
    private void initialize() {
        listSpinner.visibleProperty().bind(busy);
        addButton.disableProperty().bind(busy);
        editButton.disableProperty().bind(
                busy.or(table.getSelectionModel().selectedItemProperty().isNull()));
        deleteButton.disableProperty().bind(
                busy.or(table.getSelectionModel().selectedItemProperty().isNull()));
        messageLabel.setText(hint);
    }

    /**
     * True while the list is loading or saving. Shows the FXML spinner and disables
     * the buttons.
     * Pass this to TaskRunner when running work for this list.
     */
    public BooleanProperty busyProperty() {
        return busy;
    }

    /**
     * Adds a column into the table.
     *
     * @param title the column heading
     * @param text  turns a row's item into the text shown in this column
     */
    public void addColumn(String title, Function<T, String> text) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    /**
     * Replaces and sets the rows shown in the table.
     */
    public void setItems(List<T> items) {
        table.getItems().setAll(items);
    }

    /**
     * Sets what happens when Add is clicked, e.g. opening an empty dialog.
     */
    public void setOnAdd(Runnable action) {
        onAdd = action;
    }

    /**
     * Sets what happens when Edit is clicked. It receives selected row's item.
     */
    public void setOnEdit(Consumer<T> action) {
        onEdit = action;
    }

    /**
     * Sets what happens when Delete is clicked. It receives selected row's item.
     */
    public void setOnDelete(Consumer<T> action) {
        onDelete = action;
    }

    /**
     * Shows an error in red under the table, e.g. when a delete is refused
     * or the mainframe can't be reached.
     */
    public void showError(String message) {
        messageLabel.setText(message);
        if (!messageLabel.getStyleClass().contains(ERROR_STYLE)) {
            messageLabel.getStyleClass().add(ERROR_STYLE);
        }
    }

    /**
     * Removes any error and puts the normal hint back under the table.
     */
    public void clearError() {
        messageLabel.setText(hint);
        messageLabel.getStyleClass().remove(ERROR_STYLE);
    }

    /**
     * Sets the text shown when the table has no rows, e.g. "No addresses yet."
     */
    public void setEmptyText(String text) {
        table.setPlaceholder(new Label(text));
    }

    /**
     * Shows or hides the Edit button, for lists whose rows can't be updated.
     */
    public void setEditVisible(boolean visible) {
        editButton.setVisible(visible);
        editButton.setManaged(visible);
        hint = visible ? HINT : HINT_NO_EDIT;
        clearError();
    }

    @FXML
    private void handleAdd() {
        clearError();
        onAdd.run();
    }

    @FXML
    private void handleEdit() {
        clearError();
        onEdit.accept(table.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleDelete() {
        clearError();
        T selected = table.getSelectionModel().getSelectedItem();
        if (confirmDelete()) {
            onDelete.accept(selected);
        }
    }

    private boolean confirmDelete() {
        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete the selected row? This can't be undone.", delete, ButtonType.CANCEL);
        alert.setTitle("Confirm delete");
        alert.setHeaderText(null);
        alert.initOwner(table.getScene().getWindow());
        alert.getDialogPane().getStylesheets().addAll(table.getScene().getStylesheets());
        return alert.showAndWait().orElse(ButtonType.CANCEL) == delete;
    }
}