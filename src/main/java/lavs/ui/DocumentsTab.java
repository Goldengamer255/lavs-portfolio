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

/** Add, replace, and delete customer document metadata from the customer screen. */
public final class DocumentsTab {
    private static final String DIALOG = "/lavs/view/DocumentDialog.fxml";
    private static final int PAGE_SIZE = 50;

    private final EntityListController<CustomerDocumentSummary> list;
    private final DocumentService documents;
    private final TaskRunner tasks;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private String customerNumber;

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
                        form.showError("Save not confirmed: " + result.errorMessage()
                            + ". Close this form and refresh the list before trying again.");
                    }
                }, error -> form.showError("Save not confirmed. Close this form and refresh the list before trying again."));
        } else {
            tasks.run(() -> documents.replace(customerNumber, existing.id(), metadata), saving,
                result -> showReplacement(result, existing.id(), form, close),
                error -> form.showError("Couldn't replace document. Refresh the list before retrying."));
        }
    }

    private void showReplacement(DocumentReplacementResult result, long oldId,
            DocumentDialogController form, Runnable close) {
        if (result.isSuccess()) {
            close.run();
            load(customerNumber);
        } else if (result.isPartial()) {
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
