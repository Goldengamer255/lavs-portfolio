package lavs.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lavs.domain.CustomerDocument;
import lavs.domain.CustomerDocumentSummary;
import lavs.domain.DocumentMetadata;
import lavs.domain.Page;
import lavs.gateway.OperationResult;
import lavs.service.DocumentReplacementResult;
import lavs.service.DocumentService;
import lavs.ui.support.EntityListController;
import lavs.ui.support.FormDialog;
import lavs.ui.support.TaskRunner;

// AI-assisted: adapted the team's list-and-dialog pattern for document metadata and replacement outcomes.
/**
 * Coordinates listing, adding, replacing, and deleting customer document metadata.
 * No file contents are uploaded by this tab.
 *
 * <p>Service operations run through {@link TaskRunner}, with completion handled
 * on the JavaFX application thread. Replacement outcomes distinguish a complete
 * update from an added replacement whose original could not be confirmed deleted.</p>
 */
public final class DocumentsTab {
    private static final String DIALOG = "/lavs/view/DocumentDialog.fxml";
    private static final int PAGE_SIZE = 50;

    private final EntityListController<CustomerDocumentSummary> list;
    private final DocumentService documents;
    private final TaskRunner tasks;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private String customerNumber;

    /**
     * Configures document columns and connects list actions to the metadata workflow.
     * Must be constructed on the JavaFX application thread.
     *
     * @param list document list UI and operation error display
     * @param documents service providing document metadata operations
     * @param tasks runner for background requests and UI completion callbacks
     */
    public DocumentsTab(EntityListController<CustomerDocumentSummary> list,
            DocumentService documents, TaskRunner tasks) {
        this.list = list;
        this.documents = documents;
        this.tasks = tasks;
        list.addColumn("Type", CustomerDocumentSummary::type);
        list.addColumn("File name", CustomerDocumentSummary::fileName);
        list.setEmptyText("No documents yet.");
        list.setOnAdd(() -> openDialog(null));
        list.setOnEdit(this::loadForEdit);
        list.setOnDelete(this::delete);
    }

    /**
     * Loads the first page of document summaries for the selected customer.
     * Must be called on the JavaFX application thread.
     *
     * @param customerNumber customer whose documents should be displayed
     */
    public void load(String customerNumber) {
        this.customerNumber = customerNumber;
        list.clearError();
        tasks.run(() -> documents.list(customerNumber, 1, PAGE_SIZE), list.busyProperty(),
            this::showList, error -> list.showError("Couldn't load documents. Try again."));
    }

    private void showList(OperationResult<Page<CustomerDocumentSummary>> result) {
        if (result.isSuccess()) {
            list.setItems(result.value().items());
        } else {
            list.showError("Couldn't load documents: " + result.errorMessage());
        }
    }

    /** Retrieves complete metadata because list summaries do not contain every form field. */
    private void loadForEdit(CustomerDocumentSummary summary) {
        tasks.run(() -> documents.get(customerNumber, summary.id()), list.busyProperty(),
            result -> {
                if (result.isSuccess()) {
                    openDialog(result.value());
                } else {
                    list.showError("Couldn't open document: " + result.errorMessage());
                }
            }, error -> list.showError("Couldn't open document. Try again."));
    }

    private void openDialog(CustomerDocument existing) {
        var form = new DocumentDialogController(existing);
        FormDialog.show(list.window(), existing == null ? "Add Document" : "Edit Document",
            DIALOG, form, saving, close -> save(form, existing, close));
    }

    private void save(DocumentDialogController form, CustomerDocument existing, Runnable close) {
        DocumentMetadata metadata = form.read();
        // The form reports invalid input itself; do not submit incomplete metadata.
        if (metadata == null) {
            return;
        }
        if (existing == null) {
            tasks.run(() -> documents.add(customerNumber, metadata), saving,
                result -> {
                    if (result.isSuccess()) {
                        close.run();
                        load(customerNumber);
                    } else {
                        // An unconfirmed response may still represent a persisted add.
                        // Ask for reconciliation before another save can create a duplicate.
                        form.showError("Save not confirmed: " + result.errorMessage()
                            + ". Close this form and refresh the list before trying again.");
                    }
                }, error -> form.showError("Save not confirmed. Close this form and refresh the list before trying again."));
        } else {
            // Replacement adds the new metadata before deleting the original,
            // so completion must account for a partially successful operation.
            tasks.run(() -> documents.replace(customerNumber, existing.id(), metadata), saving,
                result -> showReplacement(result, existing.id(), form, close),
                error -> form.showError("Couldn't replace document. Refresh the list before retrying."));
        }
    }

    /** Resolves replacement outcomes without automatically repeating a potentially completed add. */
    private void showReplacement(DocumentReplacementResult result, long oldId,
            DocumentDialogController form, Runnable close) {
        if (result.isSuccess()) {
            close.run();
            load(customerNumber);
        } else if (result.isPartial()) {
            // The replacement exists. Close the form and identify both records
            // so the user can resolve any duplicate from the refreshed list.
            close.run();
            load(customerNumber);
            list.showError("Replacement #" + result.replacement().id()
                + " was added, but deletion of #" + oldId
                + " was not confirmed. Check the list and delete any duplicate. Do not save again.");
        } else {
            form.showError("Replacement not confirmed: " + result.errorMessage()
                + ". Close this form and refresh the list before trying again.");
        }
    }

    private void delete(CustomerDocumentSummary document) {
        tasks.run(() -> documents.delete(customerNumber, document.id()), list.busyProperty(),
            result -> {
                if (result.isSuccess()) {
                    load(customerNumber);
                } else {
                    list.showError("Couldn't delete document: " + result.errorMessage());
                }
            }, error -> list.showError("Couldn't delete document. Refresh the list before retrying."));
    }
}
