package lavs.ui;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lavs.domain.Page;
import lavs.domain.Relationship;
import lavs.gateway.OperationResult;
import lavs.service.CategoryService;
import lavs.service.CustomerService;
import lavs.service.RelationshipInput;
import lavs.service.SaveResult;
import lavs.ui.support.EntityListController;
import lavs.ui.support.FormDialog;
import lavs.ui.support.TaskRunner;

/** Manages the customer Relationships tab and its CRUD workflow. */
public final class RelationshipsTab {

    private static final String RELATIONSHIP_TYPES = "CustomerRelationshipType";
    private static final String DIALOG = "/lavs/view/RelationshipDialog.fxml";
    private static final int PAGE_SIZE = 50;
    private static final String LOAD_FAILED =
            "Couldn't load relationships. Check your connection and try again.";
    private static final String SAVE_FAILED =
            "Not saved yet. The mainframe couldn't be reached. Try Save again.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String DELETE_FAILED =
            "Couldn't delete the relationship. Check your connection and try again.";
    private static final String TYPES_FAILED =
            "Couldn't load relationship types. Press Add or Edit to try again.";

    private final EntityListController<Relationship> list;
    private final CustomerService customers;
    private final CategoryService categories;
    private final TaskRunner tasks;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);

    private String customerNumber;
    private List<String> types = List.of();

    public RelationshipsTab(EntityListController<Relationship> list,
            CustomerService customers, CategoryService categories, TaskRunner tasks) {
        this.list = list;
        this.customers = customers;
        this.categories = categories;
        this.tasks = tasks;

        list.addColumn("Customer No.", Relationship::secondaryCustomerNumber);
        list.addColumn("Customer", relationship -> valueOrEmpty(
                relationship.secondaryCustomerName()));
        list.addColumn("Relationship", relationship -> "Other".equals(relationship.type())
                ? relationship.otherType() : relationship.type());
        list.setEmptyText("No relationships yet.");
        list.setOnAdd(() -> openDialog(null));
        list.setOnEdit(this::openDialog);
        list.setOnDelete(this::delete);
    }

    public void load(String customerNumber) {
        this.customerNumber = customerNumber;
        list.clearError();
        tasks.run(() -> customers.listRelationships(customerNumber, 1, PAGE_SIZE),
                list.busyProperty(), this::showRelationships,
                error -> list.showError(LOAD_FAILED));
        if (types.isEmpty()) {
            loadTypes(() -> {
            });
        }
    }

    private void openDialog(Relationship existing) {
        if (types.isEmpty()) {
            list.clearError();
            loadTypes(() -> openDialog(existing));
            return;
        }
        String number = customerNumber;
        RelationshipDialogController form = new RelationshipDialogController(
                types, existing, input -> customers.validateRelationship(number, input));
        String title = existing == null ? "Add Relationship" : "Edit Relationship";
        FormDialog.show(list.window(), title, DIALOG, form, saving,
                close -> save(form, existing, close));
    }

    private void loadTypes(Runnable then) {
        tasks.run(() -> categories.getCategoryValues(RELATIONSHIP_TYPES),
                list.busyProperty(), result -> showTypes(result, then),
                error -> list.showError(TYPES_FAILED));
    }

    private void showTypes(OperationResult<List<String>> result, Runnable then) {
        if (!result.isSuccess() || result.value().isEmpty()) {
            list.showError(TYPES_FAILED);
            return;
        }
        types = result.value();
        then.run();
    }

    private void save(RelationshipDialogController form,
            Relationship existing, Runnable close) {
        RelationshipInput input = form.read();
        String number = customerNumber;
        tasks.run(() -> existing == null
                ? customers.addRelationship(number, input)
                : customers.updateRelationship(number, existing.id(), input),
                saving, result -> showSaveResult(result, form, close),
                error -> form.showFormError(SAVE_FAILED));
    }

    private void showRelationships(OperationResult<Page<Relationship>> result) {
        if (!result.isSuccess()) {
            list.showError(LOAD_FAILED);
        } else {
            list.setItems(result.value().items());
        }
    }

    private void showSaveResult(OperationResult<SaveResult<Relationship>> result,
            RelationshipDialogController form, Runnable close) {
        if (!result.isSuccess()) {
            form.showFormError(result.isRejected()
                    ? SAVE_REJECTED + result.errorMessage() : SAVE_FAILED);
            return;
        }
        SaveResult<Relationship> saved = result.value();
        if (saved.value() == null) {
            form.showErrors(saved.validation());
            return;
        }
        close.run();
        load(customerNumber);
    }

    private void delete(Relationship relationship) {
        tasks.run(() -> customers.deleteRelationship(customerNumber, relationship.id()),
                list.busyProperty(), this::showDeleteResult,
                error -> list.showError(DELETE_FAILED));
    }

    private void showDeleteResult(OperationResult<Boolean> result) {
        if (result.isSuccess()) {
            load(customerNumber);
        } else if (result.isRejected()) {
            list.showError(result.errorMessage());
        } else {
            list.showError(DELETE_FAILED);
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
