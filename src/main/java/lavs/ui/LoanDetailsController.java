package lavs.ui;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import lavs.domain.CoBorrower;
import lavs.domain.Loan;
import lavs.domain.PaymentSummary;
import lavs.gateway.OperationResult;
import lavs.service.*;
import lavs.ui.support.Dialogs;
import lavs.ui.support.EntityListController;
import lavs.ui.support.TaskRunner;
import lavs.ui.support.ValidationFeedback;
import lavs.validation.FieldKey;
import lavs.validation.LoanField;

// AI-assisted: Built by adapting my CustomerDetailsController pattern, and having Claude tell me which parts had to change.
// Claude provided the dirty state tracking, dropdown loading, and multi-error failures map.
/**
 * Controller for the loan screen: creating a loan, editing it, changing its
 * status and managing its co-borrowers.
 */
public final class LoanDetailsController implements LoanContextAware, ScreenLifecycle {

    private static final String RATE_TYPES = "LoanRateType";
    private static final String RATE_FREQUENCIES = "LoanRateFrequency";
    private static final String PAYMENT_FREQUENCIES = "LoanPaymentFrequency";
    private static final String LOAD_FAILED = "Couldn't load this loan. Check your connection and try again.";
    private static final String OPTIONS_FAILED = "Couldn't load the dropdown options. Check your connection and try again.";
    private static final String SAVE_FAILED = "Not saved yet. The mainframe couldn't be reached.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String STATUS_FAILED = "Status not changed. The mainframe couldn't be reached.";
    private static final String STATUS_REJECTED = "Status not changed. The mainframe said: ";

    private final LoanService loans;
    private final CategoryService categories;
    private final Navigator navigator;
    private final TaskRunner tasks;
    private final ValidationFeedback feedback = new ValidationFeedback();
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private final BooleanProperty hasLoan = new SimpleBooleanProperty(false);
    private final BooleanProperty creatingNew = new SimpleBooleanProperty(false);
    private final Map<String, Runnable> failures = new LinkedHashMap<>();
    private final PaymentService payments;

    private LoanScreenContext context;
    private Loan loan;
    private String status;
    private boolean started;
    private boolean filling;
    private boolean dirty;
    private CoBorrowersTab coBorrowersTab;
    private PaymentsTab paymentsTab;

    @FXML
    private Button backButton;
    @FXML
    private Label loanTitleLabel;
    @FXML
    private Label applicantLabel;
    @FXML
    private ProgressIndicator loadingSpinner;
    @FXML
    private Label statusLabel;
    @FXML
    private Button changeStatusButton;
    @FXML
    private HBox saveErrorBanner;
    @FXML
    private Label saveErrorLabel;
    @FXML
    private Button retryButton;
    @FXML
    private TextField principalField;
    @FXML
    private Label principalError;
    @FXML
    private ComboBox<String> rateFrequencyCombo;
    @FXML
    private Label rateFrequencyError;
    @FXML
    private TextField paymentAmountField;
    @FXML
    private Label paymentAmountError;
    @FXML
    private TextField rateField;
    @FXML
    private Label rateError;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private Label startDateError;
    @FXML
    private ComboBox<String> paymentFrequencyCombo;
    @FXML
    private Label paymentFrequencyError;
    @FXML
    private ComboBox<String> rateTypeCombo;
    @FXML
    private Label rateTypeError;
    @FXML
    private DatePicker renewalDatePicker;
    @FXML
    private Label renewalDateError;
    @FXML
    private Label formErrorLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button viewSummaryButton;
    @FXML
    private ProgressIndicator saveSpinner;
    @FXML
    private Node coborrowersList;
    @FXML
    private EntityListController<CoBorrower> coborrowersListController;
    @FXML
    private Node paymentsList;
    @FXML
    private EntityListController<PaymentSummary> paymentsListController;

    /**
     * Creates the controller with the services it needs. The worker is the shared
     * background executor from the application shell.
     */
    public LoanDetailsController(LoanService loans, CategoryService categories, PaymentService payments,
            Navigator navigator, Executor worker) {
        this.loans = loans;
        this.categories = categories;
        this.payments = payments;
        this.navigator = navigator;
        this.tasks = new TaskRunner(worker);
    }

    @Override
    public void setLoanContext(LoanScreenContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        BooleanBinding locked = loading.or(saving);
        BooleanBinding formLocked = locked.or(hasLoan.not().and(creatingNew.not()));
        loadingSpinner.visibleProperty().bind(loading);
        saveSpinner.visibleProperty().bind(saving);
        saveErrorBanner.managedProperty().bind(saveErrorBanner.visibleProperty());

        for (TextField field : List.of(principalField, paymentAmountField, rateField)) {
            field.editableProperty().bind(formLocked.not());
        }
        for (Control control : List.of(rateTypeCombo, rateFrequencyCombo, paymentFrequencyCombo,
                startDatePicker, renewalDatePicker)) {
            control.disableProperty().bind(formLocked);
        }
        startDatePicker.setEditable(false);
        renewalDatePicker.setEditable(false);
        saveButton.disableProperty().bind(formLocked);
        retryButton.disableProperty().bind(locked);
        backButton.disableProperty().bind(saving);
        viewSummaryButton.disableProperty().bind(hasLoan.not().or(saving));
        changeStatusButton.disableProperty().bind(hasLoan.not().or(formLocked));
        coborrowersList.disableProperty().bind(hasLoan.not());

        feedback.register(LoanField.PRINCIPAL.key(), principalField, principalError);
        feedback.register(LoanField.RATE_AMOUNT.key(), rateField, rateError);
        feedback.register(LoanField.RATE_TYPE.key(), rateTypeCombo, rateTypeError);
        feedback.register(LoanField.RATE_FREQUENCY.key(), rateFrequencyCombo, rateFrequencyError);
        feedback.register(LoanField.PAYMENT_AMOUNT.key(), paymentAmountField, paymentAmountError);
        feedback.register(LoanField.PAYMENT_FREQUENCY.key(), paymentFrequencyCombo, paymentFrequencyError);
        feedback.register(LoanField.START_DATE.key(), startDatePicker, startDateError);
        feedback.register(LoanField.RENEWAL_DATE.key(), renewalDatePicker, renewalDateError);
        feedback.setFormErrorLabel(formErrorLabel);
        feedback.setOnFieldLeft(() -> feedback.showErrors(loans.validateLoan(readForm())));

        // Any change the user makes counts as unsaved; filling the form from the
        // mainframe doesn't.
        List.<ObservableValue<?>>of(principalField.textProperty(), paymentAmountField.textProperty(),
                rateField.textProperty(),
                rateTypeCombo.valueProperty(), rateFrequencyCombo.valueProperty(),
                paymentFrequencyCombo.valueProperty(), startDatePicker.valueProperty(),
                renewalDatePicker.valueProperty())
                .forEach(property -> property.addListener((observable, oldValue, newValue) -> {
                    if (!filling) {
                        dirty = true;
                    }
                }));

        coBorrowersTab = new CoBorrowersTab(coborrowersListController, loans, categories, navigator, tasks);
        paymentsTab = new PaymentsTab(paymentsListController, payments, tasks);
        paymentsList.disableProperty().bind(hasLoan.not());
    }

    /**
     * Loads the loan, or starts a blank form for a new one. When the user comes
     * back to this screen with unsaved changes, those are kept and only the
     * co-borrowers are refreshed.
     */
    @Override
    public void onShow() {
        if (rateTypeCombo.getItems().isEmpty() || rateFrequencyCombo.getItems().isEmpty()
                || paymentFrequencyCombo.getItems().isEmpty()) {
            loadOptions();
        }
        if (loan == null && context.mode() == ScreenMode.NEW) {
            if (!started) {
                startNewLoan();
            }
            return;
        }
        if (dirty && loan != null) {
            coBorrowersTab.load(loan.loanNumber());
            paymentsTab.load(loan.loanNumber());
            return;
        }
        loadLoan();
    }

    private void startNewLoan() {
        started = true;
        filling = true;
        loanTitleLabel.setText("New Loan");
        applicantLabel.setText("Applicant: " + context.customerNumber());
        statusLabel.setText("Status: -");
        principalField.clear();
        paymentAmountField.clear();
        rateField.clear();
        rateTypeCombo.setValue(null);
        rateFrequencyCombo.setValue(null);
        paymentFrequencyCombo.setValue(null);
        startDatePicker.setValue(null);
        renewalDatePicker.setValue(null);
        filling = false;
        dirty = false;
        hasLoan.set(false);
        creatingNew.set(true);
        feedback.clear();
    }

    private void loadLoan() {
        clearFailure(LOAD_FAILED);
        String number = context.loanNumber();
        tasks.run(() -> loans.get(number), loading, this::showLoan,
                error -> showFailure(LOAD_FAILED, this::loadLoan));
    }

    private void showLoan(OperationResult<Loan> result) {
        if (!result.isSuccess()) {
            showFailure(LOAD_FAILED, this::loadLoan);
            return;
        }
        fillForm(result.value());
        coBorrowersTab.load(loan.loanNumber());
        paymentsTab.load(loan.loanNumber());
    }

    private void fillForm(Loan loaded) {
        loan = loaded;
        status = loaded.status();
        filling = true;
        loanTitleLabel.setText("Loan " + loaded.loanNumber());
        applicantLabel.setText("Applicant: " + loaded.customerNumber());
        statusLabel.setText("Status: " + status);
        principalField.setText(plain(loaded.principal()));
        paymentAmountField.setText(plain(loaded.paymentAmount()));
        rateField.setText(plain(loaded.rateAmount()));
        rateTypeCombo.setValue(loaded.rateType());
        rateFrequencyCombo.setValue(loaded.rateFrequency());
        paymentFrequencyCombo.setValue(loaded.paymentFrequency());
        startDatePicker.setValue(loaded.startDate());
        renewalDatePicker.setValue(loaded.renewalDate());
        filling = false;
        dirty = false;
        hasLoan.set(true);
        creatingNew.set(false);
        feedback.clear();
    }

    private LoanInput readForm() {
        return new LoanInput(principalField.getText().trim(), paymentAmountField.getText().trim(),
                rateField.getText().trim(), rateTypeCombo.getValue(), rateFrequencyCombo.getValue(),
                paymentFrequencyCombo.getValue(), startDatePicker.getValue(), renewalDatePicker.getValue());
    }

    // Shows 250000 rather than 250000.0 in the form.
    private static String plain(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private void loadOptions() {
        clearFailure(OPTIONS_FAILED);
        loadOptions(RATE_TYPES, rateTypeCombo);
        loadOptions(RATE_FREQUENCIES, rateFrequencyCombo);
        loadOptions(PAYMENT_FREQUENCIES, paymentFrequencyCombo);
    }

    private void loadOptions(String category, ComboBox<String> combo) {
        tasks.run(() -> categories.getCategoryValues(category), loading,
                result -> showOptions(result, combo), error -> showFailure(OPTIONS_FAILED, this::loadOptions));
    }

    private void showOptions(OperationResult<List<String>> result, ComboBox<String> combo) {
        if (!result.isSuccess() || result.value().isEmpty()) {
            showFailure(OPTIONS_FAILED, this::loadOptions);
            return;
        }
        // Keeps a value that was already filled in, and doesn't count as a user change.
        String selected = combo.getValue();
        filling = true;
        combo.getItems().setAll(result.value());
        combo.setValue(selected);
        filling = false;
    }

    @FXML
    private void handleSave() {
        clearFailure(SAVE_FAILED);
        LoanInput input = readForm();
        boolean creating = context.mode() == ScreenMode.NEW && loan == null;
        if (!creating && loan == null) {
            return;
        }
        String customerNumber = context.customerNumber();
        String loanNumber = creating ? null : loan.loanNumber();
        tasks.run(() -> creating ? loans.create(customerNumber, input) : loans.update(loanNumber, input),
                saving, this::showSaveResult, error -> showFailure(SAVE_FAILED, this::handleSave));
    }

    private void showSaveResult(OperationResult<SaveResult<Loan>> result) {
        if (!result.isSuccess()) {
            if (result.isRejected()) {
                showFormError(SAVE_REJECTED + result.errorMessage());
            } else {
                showFailure(SAVE_FAILED, this::handleSave);
            }
            return;
        }
        SaveResult<Loan> saved = result.value();
        if (saved.value() == null) {
            feedback.showAllErrors(saved.validation());
            return;
        }
        Loan savedLoan = saved.value();
        context = new LoanScreenContext(ScreenMode.EXISTING, savedLoan.customerNumber(), savedLoan.loanNumber());
        fillForm(savedLoan);
        coBorrowersTab.load(savedLoan.loanNumber());
        paymentsTab.load(savedLoan.loanNumber());
    }

    @FXML
    private void handleChangeStatus() {
        clearFailure(STATUS_FAILED);
        String number = loan.loanNumber();
        tasks.run(() -> loans.allowedNextStatuses(number), loading, this::chooseStatus,
                error -> showFailure(STATUS_FAILED, this::handleChangeStatus));
    }

    private void chooseStatus(OperationResult<List<String>> result) {
        if (!result.isSuccess()) {
            showFailure(STATUS_FAILED, this::handleChangeStatus);
            return;
        }
        List<String> allowed = result.value();
        if (allowed.isEmpty()) {
            Dialogs.showInfo(window(), "Change Status", "This loan can't move on from " + status + ".");
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(allowed.get(0), allowed);
        dialog.setTitle("Change Status");
        dialog.setHeaderText("Current status: " + status);
        dialog.setContentText("New status:");
        dialog.initOwner(window());
        dialog.getDialogPane().getStylesheets().addAll(window().getScene().getStylesheets());
        dialog.showAndWait().ifPresent(this::changeStatus);
    }

    private void changeStatus(String newStatus) {
        clearFailure(STATUS_FAILED);
        String number = loan.loanNumber();
        tasks.run(() -> loans.changeStatus(number, newStatus), saving,
                result -> showStatusResult(result, newStatus),
                error -> showFailure(STATUS_FAILED, () -> changeStatus(newStatus)));
    }

    private void showStatusResult(OperationResult<String> result, String newStatus) {
        if (result.isSuccess()) {
            status = result.value();
            statusLabel.setText("Status: " + status);
        } else if (result.isRejected()) {
            showFormError(STATUS_REJECTED + result.errorMessage());
        } else {
            showFailure(STATUS_FAILED, () -> changeStatus(newStatus));
        }
    }

    @FXML
    private void handleViewSummary() {
        navigator.showLoanSummary(loan.loanNumber());
    }

    @FXML
    private void handleBack() {
        if (dirty && !Dialogs.confirmDiscard(window())) {
            return;
        }
        navigator.back();
    }

    @FXML
    private void handleRetry() {
        List<Runnable> retries = List.copyOf(failures.values());
        failures.clear();
        updateBanner();
        retries.forEach(Runnable::run);
    }

    // Each failing request keeps its own message and retry, so one success never
    // hides another failure.
    private void showFailure(String message, Runnable retry) {
        failures.put(message, retry);
        updateBanner();
    }

    private void clearFailure(String message) {
        failures.remove(message);
        updateBanner();
    }

    private void updateBanner() {
        saveErrorLabel.setText(String.join(" ", failures.keySet()));
        saveErrorBanner.setVisible(!failures.isEmpty());
    }

    private void showFormError(String message) {
        feedback.showAllErrors(new ValidationReport(List.of(new ValidationIssue(FieldKey.GLOBAL, message))));
    }

    private Window window() {
        return saveButton.getScene().getWindow();
    }
}