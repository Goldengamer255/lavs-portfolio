package lavs.ui;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lavs.domain.Phone;
import lavs.domain.Page;
import lavs.gateway.OperationResult;
import lavs.service.PhoneInput;
import lavs.service.CategoryService;
import lavs.service.CustomerService;
import lavs.service.SaveResult;
import lavs.ui.support.EntityListController;
import lavs.ui.support.FormDialog;
import lavs.ui.support.TaskRunner;

/**
 * Manages phone numbers tab: loading, the add/edit popup, saving and deleting.
 */
public final class PhonesTab {

    private static final String PHONE_TYPES = "CustomerPhoneNumberType";
    private static final String DIALOG = "/lavs/view/PhoneDialog.fxml";
    private static final int PAGE_SIZE = 50;
    private static final String LOAD_FAILED = "Couldn't load phone numbers. Check your connection and try again.";
    private static final String SAVE_FAILED = "Not saved yet. The mainframe couldn't be reached. Try Save again.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String DELETE_FAILED = "Couldn't delete the phone number. Check your connection and try again.";
    private static final String TYPES_FAILED = "Couldn't load the phone types. Press Add or Edit to try again.";

    private final EntityListController<Phone> list;
    private final CustomerService customers;
    private final CategoryService categories;
    private final TaskRunner tasks;
    private final Runnable onChanged;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);

    private String customerNumber;
    private List<String> types = List.of();

    /**
     * Sets up the phone list's columns and buttons.
     */
    public PhonesTab(EntityListController<Phone> list, CustomerService customers,
            CategoryService categories, TaskRunner tasks, Runnable onChanged) {
        this.list = list;
        this.customers = customers;
        this.categories = categories;
        this.tasks = tasks;
        this.onChanged = onChanged;

        list.addColumn("Type", p -> "Other".equals(p.type()) ? p.otherType() : p.type());
        list.addColumn("Number", p -> p.countryCode() + " " + p.number());
        list.addColumn("Primary", p -> p.isPrimary() ? "Yes" : "");
        list.addColumn("Can text", p -> p.canTxt() ? "Yes" : "");

        list.setEmptyText("No phone numbers yet.");
        list.setOnAdd(() -> openDialog(null));
        list.setOnEdit(this::openDialog);
        list.setOnDelete(this::delete);
    }

    /**
     * Loads customer's phone numbers, and phone types for the dropdown on first
     * time.
     */
    public void load(String customerNumber) {
        this.customerNumber = customerNumber;
        list.clearError();
        tasks.run(() -> customers.listPhones(customerNumber, 1, PAGE_SIZE), list.busyProperty(),
                this::showPhones, error -> list.showError(LOAD_FAILED));
        if (types.isEmpty()) {
            loadTypes(() -> {
            });
        }
    }

    private void openDialog(Phone existing) {
        if (types.isEmpty()) {
            list.clearError();
            loadTypes(() -> openDialog(existing));
            return;
        }
        PhoneDialogController form = new PhoneDialogController(types, existing, customers::validatePhone);
        String title = existing == null ? "Add Phone Number" : "Edit Phone Number";
        FormDialog.show(list.window(), title, DIALOG, form, saving, close -> save(form, existing, close));
    }

    private void loadTypes(Runnable then) {
        tasks.run(() -> categories.getCategoryValues(PHONE_TYPES), list.busyProperty(),
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

    private void save(PhoneDialogController form, Phone existing, Runnable close) {
        PhoneInput input = form.read();
        String number = customerNumber;
        tasks.run(() -> existing == null
                ? customers.addPhone(number, input)
                : customers.updatePhone(number, existing.id(), input),
                saving, result -> showSaveResult(result, form, close),
                error -> form.showFormError(SAVE_FAILED));
    }

    private void showPhones(OperationResult<Page<Phone>> result) {
        if (!result.isSuccess()) {
            list.showError(LOAD_FAILED);
        } else {
            list.setItems(result.value().items());
        }
    }

    private void showSaveResult(OperationResult<SaveResult<Phone>> result, PhoneDialogController form,
            Runnable close) {
        if (!result.isSuccess()) {
            if (result.isRejected()) {
                form.showFormError(SAVE_REJECTED + result.errorMessage());
            } else {
                form.showFormError(SAVE_FAILED);
            }
            return;
        }

        SaveResult<Phone> saved = result.value();
        if (saved.value() == null) {
            form.showErrors(saved.validation());
            return;
        }

        close.run();
        load(customerNumber);
        onChanged.run();
    }

    private void delete(Phone phone) {
        tasks.run(() -> customers.deletePhone(customerNumber, phone.id()),
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