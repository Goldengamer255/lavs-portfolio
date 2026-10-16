package lavs.ui;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lavs.domain.Employer;
import lavs.domain.Page;
import lavs.gateway.OperationResult;
import lavs.service.EmployerInput;
import lavs.service.CategoryService;
import lavs.service.CustomerService;
import lavs.service.SaveResult;
import lavs.ui.support.EntityListController;
import lavs.ui.support.FormDialog;
import lavs.ui.support.TaskRunner;

/**
 * Manages employers tab: loading, the add/edit popup, saving and deleting.
 */
public final class EmployersTab {

    private static final String EMPLOYER_TYPES = "CustomerEmployerType";
    private static final String DIALOG = "/lavs/view/EmployerDialog.fxml";
    private static final int PAGE_SIZE = 50;
    private static final String LOAD_FAILED = "Couldn't load employers. Check your connection and try again.";
    private static final String SAVE_FAILED = "Not saved yet. The mainframe couldn't be reached. Try Save again.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String DELETE_FAILED = "Couldn't delete the employer. Check your connection and try again.";
    private static final String TYPES_FAILED = "Couldn't load the employer types. Press Add or Edit to try again.";
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-NZ"));

    private final EntityListController<Employer> list;
    private final CustomerService customers;
    private final CategoryService categories;
    private final TaskRunner tasks;
    private final Runnable onChanged;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);

    private String customerNumber;
    private List<String> types = List.of();

    /**
     * Sets up the employer list's columns and buttons.
     */
    public EmployersTab(EntityListController<Employer> list, CustomerService customers,
            CategoryService categories, TaskRunner tasks, Runnable onChanged) {
        this.list = list;
        this.customers = customers;
        this.categories = categories;
        this.tasks = tasks;
        this.onChanged = onChanged;

        list.addColumn("Type", Employer::type);
        list.addColumn("Name", Employer::name);
        list.addColumn("Position", Employer::position);
        list.addColumn("Salary", e -> MONEY_FORMAT.format(e.salary()));

        list.setEmptyText("No employers yet.");
        list.setOnAdd(() -> openDialog(null));
        list.setOnEdit(this::openDialog);
        list.setOnDelete(this::delete);
    }

    /**
     * Loads customer's employers, and employer types for the dropdown on first
     * time.
     */
    public void load(String customerNumber) {
        this.customerNumber = customerNumber;
        list.clearError();
        tasks.run(() -> customers.listEmployers(customerNumber, 1, PAGE_SIZE), list.busyProperty(),
                this::showEmployers, error -> list.showError(LOAD_FAILED));
        if (types.isEmpty()) {
            loadTypes(() -> {
            });
        }
    }

    private void openDialog(Employer existing) {
        if (types.isEmpty()) {
            list.clearError();
            loadTypes(() -> openDialog(existing));
            return;
        }
        EmployerDialogController form = new EmployerDialogController(types, existing, customers::validateEmployer);
        String title = existing == null ? "Add Employer" : "Edit Employer";
        FormDialog.show(list.window(), title, DIALOG, form, saving, close -> save(form, existing, close));
    }

    private void loadTypes(Runnable then) {
        tasks.run(() -> categories.getCategoryValues(EMPLOYER_TYPES), list.busyProperty(),
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

    private void save(EmployerDialogController form, Employer existing, Runnable close) {
        EmployerInput input = form.read();
        String number = customerNumber;
        tasks.run(() -> existing == null
                ? customers.addEmployer(number, input)
                : customers.updateEmployer(number, existing.id(), input),
                saving, result -> showSaveResult(result, form, close),
                error -> form.showFormError(SAVE_FAILED));
    }

    private void showEmployers(OperationResult<Page<Employer>> result) {
        if (!result.isSuccess()) {
            list.showError(LOAD_FAILED);
        } else {
            list.setItems(result.value().items());
        }
    }

    private void showSaveResult(OperationResult<SaveResult<Employer>> result, EmployerDialogController form,
            Runnable close) {
        if (!result.isSuccess()) {
            if (result.isRejected()) {
                form.showFormError(SAVE_REJECTED + result.errorMessage());
            } else {
                form.showFormError(SAVE_FAILED);
            }
            return;
        }

        SaveResult<Employer> saved = result.value();
        if (saved.value() == null) {
            form.showErrors(saved.validation());
            return;
        }

        close.run();
        load(customerNumber);
        onChanged.run();
    }

    private void delete(Employer employer) {
        tasks.run(() -> customers.deleteEmployer(customerNumber, employer.id()),
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