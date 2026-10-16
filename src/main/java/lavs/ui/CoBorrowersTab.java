package lavs.ui;

import java.util.List;
import java.util.Optional;
import javafx.scene.control.ChoiceDialog;
import lavs.domain.CoBorrower;
import lavs.domain.Page;
import lavs.gateway.OperationResult;
import lavs.service.CategoryService;
import lavs.service.LoanService;
import lavs.ui.support.EntityListController;
import lavs.ui.support.TaskRunner;

// Built by adapting my AddressesTab pattern, and having claude tell me which parts had to change. 
/**
 * Manages the co-borrowers list on the loan screen: loading, linking a customer
 * through the co-borrower picker, changing a link's type and removing a link.
 */
public final class CoBorrowersTab {

    private static final String COBORROWER_TYPES = "LoanCoborrowerType";
    private static final int PAGE_SIZE = 50;
    private static final String LOAD_FAILED = "Couldn't load the co-borrowers. Check your connection and try again.";
    private static final String TYPES_FAILED = "Couldn't load the co-borrower types. Press Add or Edit to try again.";
    private static final String SAVE_FAILED = "Couldn't save the co-borrower. Check your connection and try again.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String DELETE_FAILED = "Couldn't remove the co-borrower. Check your connection and try again.";
    private static final String PICK_ACTIVE = "Finish choosing the current co-borrower first.";

    private final EntityListController<CoBorrower> list;
    private final LoanService loans;
    private final CategoryService categories;
    private final Navigator navigator;
    private final TaskRunner tasks;

    private String loanNumber;
    private List<String> types = List.of();

    /**
     * Sets up the co-borrower list's columns and buttons.
     */
    public CoBorrowersTab(EntityListController<CoBorrower> list, LoanService loans,
            CategoryService categories, Navigator navigator, TaskRunner tasks) {
        this.list = list;
        this.loans = loans;
        this.categories = categories;
        this.navigator = navigator;
        this.tasks = tasks;

        list.addColumn("Name", CoBorrower::customerName);
        list.addColumn("Customer No.", CoBorrower::customerNumber);
        list.addColumn("Type", CoBorrower::type);

        list.setEmptyText("No co-borrowers yet.");
        list.setOnAdd(() -> withTypes(this::pickCustomer));
        list.setOnEdit(coBorrower -> withTypes(() -> changeType(coBorrower)));
        list.setOnDelete(this::remove);
    }

    /**
     * Loads the loan's co-borrowers, and the co-borrower types the first time.
     */
    public void load(String loanNumber) {
        this.loanNumber = loanNumber;
        list.clearError();
        tasks.run(() -> loans.listCoBorrowers(loanNumber, 1, PAGE_SIZE), list.busyProperty(),
                this::showCoBorrowers, error -> list.showError(LOAD_FAILED));
        if (types.isEmpty()) {
            loadTypes(() -> {
            });
        }
    }

    private void showCoBorrowers(OperationResult<Page<CoBorrower>> result) {
        if (!result.isSuccess()) {
            list.showError(LOAD_FAILED);
        } else {
            list.setItems(result.value().items());
        }
    }

    private void withTypes(Runnable then) {
        if (types.isEmpty()) {
            loadTypes(then);
        } else {
            then.run();
        }
    }

    private void loadTypes(Runnable then) {
        tasks.run(() -> categories.getCategoryValues(COBORROWER_TYPES), list.busyProperty(),
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

    private void pickCustomer() {
        if (navigator.isPickingCoBorrower()) {
            list.showError(PICK_ACTIVE);
            return;
        }
        String number = loanNumber;
        navigator.pickCoBorrower(number,
                customerNumber -> chooseType(null).ifPresent(type -> add(number, customerNumber, type)));
    }

    private Optional<String> chooseType(String current) {
        String initial = current != null && types.contains(current) ? current : types.get(0);
        ChoiceDialog<String> dialog = new ChoiceDialog<>(initial, types);
        dialog.setTitle("Co-borrower Type");
        dialog.setHeaderText(null);
        dialog.setContentText("Type:");
        dialog.initOwner(list.window());
        dialog.getDialogPane().getStylesheets().addAll(list.window().getScene().getStylesheets());
        return dialog.showAndWait();
    }

    private void add(String number, String customerNumber, String type) {
        tasks.run(() -> loans.addCoBorrower(number, customerNumber, type), list.busyProperty(),
                this::showChangeResult, error -> showChangeFailure(SAVE_FAILED));
    }

    private void changeType(CoBorrower coBorrower) {
        String number = loanNumber;
        chooseType(coBorrower.type())
                .filter(type -> !type.equals(coBorrower.type()))
                .ifPresent(type -> tasks.run(
                        () -> loans.changeCoBorrowerType(number, coBorrower.id(), type),
                        list.busyProperty(), this::showChangeResult,
                        error -> showChangeFailure(SAVE_FAILED)));
    }

    private void showChangeResult(OperationResult<CoBorrower> result) {
        if (result.isSuccess()) {
            load(loanNumber);
        } else if (result.isRejected()) {
            showChangeFailure(SAVE_REJECTED + result.errorMessage());
        } else {
            showChangeFailure(SAVE_FAILED);
        }
    }

    // Reloads so partly applied changes are never left on screen, shows reason.
    private void showChangeFailure(String message) {
        load(loanNumber);
        list.showError(message);
    }

    private void remove(CoBorrower coBorrower) {
        String number = loanNumber;
        tasks.run(() -> loans.removeCoBorrower(number, coBorrower.id()), list.busyProperty(),
                this::showRemoveResult, error -> list.showError(DELETE_FAILED));
    }

    private void showRemoveResult(OperationResult<Boolean> result) {
        if (result.isSuccess()) {
            load(loanNumber);
        } else if (result.isRejected()) {
            list.showError(result.errorMessage());
        } else {
            list.showError(DELETE_FAILED);
        }
    }
}