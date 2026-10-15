package lavs.ui;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lavs.domain.Email;
import lavs.domain.Page;
import lavs.gateway.OperationResult;
import lavs.service.EmailInput;
import lavs.service.CategoryService;
import lavs.service.CustomerService;
import lavs.service.SaveResult;
import lavs.ui.support.EntityListController;
import lavs.ui.support.FormDialog;
import lavs.ui.support.TaskRunner;

/**
 * Manages email addresses tab: loading, add/edit popup, saving and deleting.
 */
public final class EmailsTab {

    private static final String EMAIL_TYPES = "CustomerEmailType";
    private static final String DIALOG = "/lavs/view/EmailDialog.fxml";
    private static final int PAGE_SIZE = 50;
    private static final String LOAD_FAILED = "Couldn't load emails. Check your connection and try again.";
    private static final String SAVE_FAILED = "Not saved yet. The mainframe couldn't be reached. Try Save again.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String DELETE_FAILED = "Couldn't delete the email. Check your connection and try again.";
    private static final String TYPES_FAILED = "Couldn't load the email types. Press Add or Edit to try again.";

    private final EntityListController<Email> list;
    private final CustomerService customers;
    private final CategoryService categories;
    private final TaskRunner tasks;
    private final Runnable onChanged;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);

    private String customerNumber;
    private List<String> types = List.of();

    /**
     * Sets up the email list's columns and buttons.
     */
    public EmailsTab(EntityListController<Email> list, CustomerService customers,
            CategoryService categories, TaskRunner tasks, Runnable onChanged) {
        this.list = list;
        this.customers = customers;
        this.categories = categories;
        this.tasks = tasks;
        this.onChanged = onChanged;

        list.addColumn("Type", e -> "Other".equals(e.type()) ? e.otherType() : e.type());
        list.addColumn("Email", Email::address);
        list.addColumn("Primary", e -> e.isPrimary() ? "Yes" : "");

        list.setEmptyText("No emails yet.");
        list.setOnAdd(() -> openDialog(null));
        list.setOnEdit(this::openDialog);
        list.setOnDelete(this::delete);
    }

    /**
     * Loads customer's email addresses, and email types for dropdown on first time.
     */
    public void load(String customerNumber) {
        this.customerNumber = customerNumber;
        list.clearError();
        tasks.run(() -> customers.listEmails(customerNumber, 1, PAGE_SIZE), list.busyProperty(),
                this::showEmails, error -> list.showError(LOAD_FAILED));
        if (types.isEmpty()) {
            loadTypes(() -> {
            });
        }
    }

    private void openDialog(Email existing) {
        if (types.isEmpty()) {
            list.clearError();
            loadTypes(() -> openDialog(existing));
            return;
        }
        EmailDialogController form = new EmailDialogController(types, existing, customers::validateEmail);
        String title = existing == null ? "Add Email" : "Edit Email";
        FormDialog.show(list.window(), title, DIALOG, form, saving, close -> save(form, existing, close));
    }

    private void loadTypes(Runnable then) {
        tasks.run(() -> categories.getCategoryValues(EMAIL_TYPES), list.busyProperty(),
                result -> showTypes(result, then), error -> list.showError(TYPES_FAILED));
    }

    private void showTypes(OperationResult<List<String>> result, Runnable then) {
        if (!result.isSuccess() || result.value().isEmpty()) {
            list.showError(TYPES_FAILED);
            return;
        }
        types = result.value();
        then.run();
    }

    private void save(EmailDialogController form, Email existing, Runnable close) {
        EmailInput input = form.read();
        String number = customerNumber;
        tasks.run(() -> existing == null
                ? customers.addEmail(number, input)
                : customers.updateEmail(number, existing.id(), input),
                saving, result -> showSaveResult(result, form, close),
                error -> form.showFormError(SAVE_FAILED));
    }

    private void showEmails(OperationResult<Page<Email>> result) {
        if (!result.isSuccess()) {
            list.showError(LOAD_FAILED);
        } else {
            list.setItems(result.value().items());
        }
    }

    private void showSaveResult(OperationResult<SaveResult<Email>> result, EmailDialogController form,
            Runnable close) {
        if (!result.isSuccess()) {
            if (result.isRejected()) {
                form.showFormError(SAVE_REJECTED + result.errorMessage());
            } else {
                form.showFormError(SAVE_FAILED);
            }
            return;
        }

        SaveResult<Email> saved = result.value();
        if (saved.value() == null) {
            form.showErrors(saved.validation());
            return;
        }

        close.run();
        load(customerNumber);
        onChanged.run();
    }

    private void delete(Email email) {
        tasks.run(() -> customers.deleteEmail(customerNumber, email.id()),
                list.busyProperty(),
                this::showDeleteResult,
                error -> list.showError(DELETE_FAILED));
    }

    private void showDeleteResult(OperationResult<Boolean> result) {
        if (result.isSuccess()) {
            load(customerNumber);
            onChanged.run();
        } else if (result.isRejected()) {
            list.showError(result.errorMessage());
        } else {
            list.showError(DELETE_FAILED);
        }
    }
}