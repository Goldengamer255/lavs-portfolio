package lavs.ui;

import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lavs.domain.Address;
import lavs.domain.Page;
import lavs.gateway.OperationResult;
import lavs.service.AddressInput;
import lavs.service.CategoryService;
import lavs.service.CustomerService;
import lavs.service.SaveResult;
import lavs.ui.support.EntityListController;
import lavs.ui.support.FormDialog;
import lavs.ui.support.TaskRunner;

// AI-assisted: mostly written by me; the loading, popup and save calls came from Claude.
/**
 * Manages the address tab interface, handling address loading, dialog creation,
 * saving, and deletion.
 */
public final class AddressesTab {

    private static final String ADDRESS_TYPES = "CustomerAddressType";
    private static final String DIALOG = "/lavs/view/AddressDialog.fxml";
    private static final int PAGE_SIZE = 50;
    private static final String LOAD_FAILED = "Couldn't load addresses. Check your connection and try again.";
    private static final String SAVE_FAILED = "Not saved yet. The mainframe couldn't be reached. Try Save again.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String DELETE_FAILED = "Couldn't delete the address. Check your connection and try again.";

    private final EntityListController<Address> list;
    private final CustomerService customers;
    private final CategoryService categories;
    private final TaskRunner tasks;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);

    private String customerNumber;
    private List<String> types = List.of();

    /**
     * Sets up the address list's columns and buttons.
     */
    public AddressesTab(EntityListController<Address> list, CustomerService customers,
            CategoryService categories, TaskRunner tasks) {
        this.list = list;
        this.customers = customers;
        this.categories = categories;
        this.tasks = tasks;

        list.addColumn("Type", a -> "Other".equals(a.type()) ? a.otherType() : a.type());
        list.addColumn("Line 1", Address::line1);
        list.addColumn("Suburb", Address::suburb);
        list.addColumn("City", Address::city);
        list.addColumn("Primary", a -> a.isPrimary() ? "Yes" : "");
        list.addColumn("Mailing", a -> a.isMailing() ? "Yes" : "");

        list.setEmptyText("No addresses yet.");
        list.setOnAdd(() -> openDialog(null));
        list.setOnEdit(this::openDialog);
        list.setOnDelete(this::delete);
    }

    /**
     * Loads customer's addresses, and address types for the dropdown on first time.
     */
    public void load(String customerNumber) {
        this.customerNumber = customerNumber;
        list.clearError();
        tasks.run(() -> customers.listAddresses(customerNumber, 1, PAGE_SIZE), list.busyProperty(),
                this::showAddresses, error -> list.showError(LOAD_FAILED));
        if (types.isEmpty()) {
            tasks.run(() -> categories.getCategoryValues(ADDRESS_TYPES), list.busyProperty(),
                    result -> types = result.isSuccess() ? result.value() : List.of(), error -> {
                    });
        }
    }

    private void openDialog(Address existing) {
        AddressDialogController form = new AddressDialogController(types, existing, customers::validateAddress);
        String title = existing == null ? "Add Address" : "Edit Address";
        FormDialog.show(list.window(), title, DIALOG, form, saving, close -> save(form, existing, close));
    }

    private void save(AddressDialogController form, Address existing, Runnable close) {
        AddressInput input = form.read();
        String number = customerNumber;
        tasks.run(() -> existing == null
                ? customers.addAddress(number, input)
                : customers.updateAddress(number, existing.id(), input),
                saving, result -> showSaveResult(result, form, close),
                error -> form.showFormError(SAVE_FAILED));
    }

    private void showAddresses(OperationResult<Page<Address>> result) {
        if (!result.isSuccess()) {
            list.showError(LOAD_FAILED);
        } else {
            list.setItems(result.value().items());
        }
    }

    private void showSaveResult(OperationResult<SaveResult<Address>> result, AddressDialogController form,
            Runnable close) {
        if (!result.isSuccess()) {
            if (result.isRejected()) {
                form.showFormError(SAVE_REJECTED + result.errorMessage());
            } else {
                form.showFormError(SAVE_FAILED);
            }
            return;
        }

        SaveResult<Address> saved = result.value();
        if (saved.value() == null) {
            form.showErrors(saved.validation());
            return;
        }

        close.run();
        load(customerNumber);
    }

    private void delete(Address address) {
        tasks.run(() -> customers.deleteAddress(customerNumber, address.id()),
                list.busyProperty(),
                this::showDeleteResult,
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
}