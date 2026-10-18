package lavs.ui;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lavs.domain.Payment;
import lavs.domain.PaymentSummary;
import lavs.gateway.OperationResult;
import lavs.service.PaymentInput;
import lavs.service.PaymentService;
import lavs.service.SaveResult;
import lavs.ui.support.EntityListController;
import lavs.ui.support.FormDialog;
import lavs.ui.support.TaskRunner;

// AI-assisted: adapted from NotesTab with help from AI; payments-specific changes (no edit, columns) written by me
/**
 * Manages the payments tab on the loan screen: loading, the add popup and deleting.
 * Payments cannot be edited because the mainframe has no update message for them.
 */
public final class PaymentsTab {

    private static final String DIALOG = "/lavs/view/PaymentDialog.fxml";
    private static final String LOAD_FAILED = "Couldn't load payments. Check your connection and try again.";
    private static final String SAVE_FAILED = "Not saved yet. The mainframe couldn't be reached. Try Save again.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String DELETE_FAILED = "Couldn't delete the payment. Check your connection and try again.";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EntityListController<PaymentSummary> list;
    private final PaymentService payments;
    private final TaskRunner tasks;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);

    private String loanNumber;

    /**
     * Sets up the payment list's columns and buttons.
     */
    public PaymentsTab(EntityListController<PaymentSummary> list, PaymentService payments, TaskRunner tasks) {
        this.list = list;
        this.payments = payments;
        this.tasks = tasks;

        list.addColumn("Date", p -> p.date() == null ? "" : p.date().format(DATE_FORMAT));
        list.addColumn("Amount Paid", p -> p.amountPaid().toPlainString());

        list.setEditVisible(false);
        list.setEmptyText("No payments yet.");
        list.setOnAdd(this::openDialog);
        list.setOnDelete(this::delete);
    }

    /**
     * Loads the loan's payments.
     */
    public void load(String loanNumber) {
        this.loanNumber = loanNumber;
        list.clearError();
        tasks.run(() -> payments.listAllPayments(loanNumber), list.busyProperty(), this::showPayments,
                error -> list.showError(LOAD_FAILED));
    }

    private void openDialog() {
        PaymentDialogController form = new PaymentDialogController(payments::validatePayment);
        FormDialog.show(list.window(), "Add Payment", DIALOG, form, saving, close -> save(form, close));
    }

    private void save(PaymentDialogController form, Runnable close) {
        PaymentInput input = form.read();
        String number = loanNumber;
        tasks.run(() -> payments.addPayment(number, input),
                saving, result -> showSaveResult(result, form, close),
                error -> form.showFormError(SAVE_FAILED));
    }

    private void showPayments(OperationResult<List<PaymentSummary>> result) {
        if (!result.isSuccess()) {
            list.showError(LOAD_FAILED);
        } else {
            list.setItems(result.value());
        }
    }

    private void showSaveResult(OperationResult<SaveResult<Payment>> result, PaymentDialogController form,
                                Runnable close) {
        if (!result.isSuccess()) {
            if (result.isRejected()) {
                form.showFormError(SAVE_REJECTED + result.errorMessage());
            } else {
                form.showFormError(SAVE_FAILED);
            }
            return;
        }

        SaveResult<Payment> saved = result.value();
        if (saved.value() == null) {
            form.showErrors(saved.validation());
            return;
        }

        close.run();
        load(loanNumber);
    }

    private void delete(PaymentSummary payment) {
        tasks.run(() -> payments.deletePayment(loanNumber, payment.id()),
                list.busyProperty(),
                this::showDeleteResult,
                error -> list.showError(DELETE_FAILED));
    }

    private void showDeleteResult(OperationResult<Boolean> result) {
        if (result.isSuccess()) {
            load(loanNumber);
        } else if (result.isRejected()) {
            list.showError(result.errorMessage());
        } else {
            list.showError(DELETE_FAILED);
        }
    }
}
