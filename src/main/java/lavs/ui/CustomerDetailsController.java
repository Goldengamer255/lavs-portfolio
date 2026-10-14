package lavs.ui;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.Locale;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import lavs.domain.Address;
import lavs.domain.Customer;
import lavs.domain.LoanSummaryItem;
import lavs.domain.Page;
import lavs.gateway.OperationResult;
import lavs.service.CategoryService;
import lavs.service.CustomerService;
import lavs.service.CustomerInput;
import lavs.service.SaveResult;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;
import lavs.ui.support.EntityListController;
import lavs.ui.support.TaskRunner;
import lavs.ui.support.ValidationFeedback;
import lavs.validation.CustomerField;

// AI-assisted: mostly written by me; the property bindings and background loading call came from Claude
/**
 * Controller for CustomerDetailsView.fxml. Shows one customer's details + tabs,
 * or a blank form for a new customer.
 */
public final class CustomerDetailsController implements CustomerContextAware, ScreenLifecycle {

    private static final String LOAD_FAILED = "Couldn't load this customer. Check your connection and try again.";
    private static final String SAVE_FAILED = "Not saved yet. The mainframe couldn't be reached.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String LOANS_FAILED = "Couldn't load this customer's loans. Check your connection and try again.";
    private static final int LOAN_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-NZ"));

    private final CustomerService customers;
    private final CategoryService categories;
    private final Navigator navigator;
    private final TaskRunner tasks;
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty editing = new SimpleBooleanProperty(false);
    private CustomerScreenContext context;
    private Customer customer;
    private Runnable retryAction = () -> {
    };
    private final ValidationFeedback feedback = new ValidationFeedback();
    private final BooleanProperty saving = new SimpleBooleanProperty(false);
    private AddressesTab addressesTab;

    @FXML
    private Label customerNameLabel;
    @FXML
    private Label customerNumberLabel;
    @FXML
    private Label customerStatusLabel;
    @FXML
    private ProgressIndicator loadingSpinner;
    @FXML
    private HBox saveErrorBanner;
    @FXML
    private Label saveErrorLabel;
    @FXML
    private TabPane detailsTabs;
    @FXML
    private TextField titleField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField familyNameField;
    @FXML
    private TextField nationalityField;
    @FXML
    private DatePicker dobPicker;
    @FXML
    private Button editButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label titleError;
    @FXML
    private Label firstNameError;
    @FXML
    private Label familyNameError;
    @FXML
    private Label dobError;
    @FXML
    private Label nationalityError;
    @FXML
    private ProgressIndicator saveSpinner;
    @FXML
    private Button linkCoborrowerButton;
    @FXML
    private Button addLoanButton;
    @FXML
    private Label loanReadinessLabel;
    @FXML
    private TableView<LoanSummaryItem> loanTable;
    @FXML
    private TableColumn<LoanSummaryItem, String> loanNumberColumn;
    @FXML
    private TableColumn<LoanSummaryItem, String> loanStatusColumn;
    @FXML
    private TableColumn<LoanSummaryItem, String> loanPrincipalColumn;
    @FXML
    private TableColumn<LoanSummaryItem, String> loanStartDateColumn;
    @FXML
    private Label formErrorLabel;
    @FXML
    private EntityListController<Address> addressesListController;

    /**
     * Creates the controller with the services it needs. The worker is the shared
     * background executor from the application shell.
     */
    public CustomerDetailsController(CustomerService customers, CategoryService categories,
            Navigator navigator, Executor worker) {
        this.customers = customers;
        this.categories = categories;
        this.navigator = navigator;
        this.tasks = new TaskRunner(worker);
    }

    @Override
    public void setCustomerContext(CustomerScreenContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        loadingSpinner.visibleProperty().bind(loading);
        saveErrorBanner.managedProperty().bind(saveErrorBanner.visibleProperty());
        for (TextField field : List.of(titleField, firstNameField, familyNameField, nationalityField)) {
            field.editableProperty().bind(editing.and(saving.not()));
        }
        dobPicker.disableProperty().bind(editing.not().or(saving));
        editButton.visibleProperty().bind(editing.not());
        editButton.managedProperty().bind(editButton.visibleProperty());
        saveButton.visibleProperty().bind(editing);
        saveButton.managedProperty().bind(saveButton.visibleProperty());
        cancelButton.visibleProperty().bind(editing);
        cancelButton.managedProperty().bind(cancelButton.visibleProperty());

        feedback.register(CustomerField.TITLE.key(), titleField, titleError);
        feedback.register(CustomerField.FIRST_NAME.key(), firstNameField, firstNameError);
        feedback.register(CustomerField.FAMILY_NAME.key(), familyNameField, familyNameError);
        feedback.register(CustomerField.DATE_OF_BIRTH.key(), dobPicker, dobError);
        feedback.register(CustomerField.NATIONALITY.key(), nationalityField, nationalityError);

        feedback.setFormErrorLabel(formErrorLabel);

        feedback.setOnFieldLeft(() -> feedback.showErrors(
                customers.validateCustomer(readForm(), customer == null)));
        saveSpinner.visibleProperty().bind(saving);
        saveButton.disableProperty().bind(saving);
        cancelButton.disableProperty().bind(saving);

        loanPrincipalColumn.setCellValueFactory(cell -> text(MONEY_FORMAT.format(cell.getValue().principal())));
        loanStartDateColumn.setCellValueFactory(cell -> text(cell.getValue().startDate() == null
                ? ""
                : cell.getValue().startDate().format(DATE_FORMAT)));
        loanNumberColumn.setCellValueFactory(cell -> text(cell.getValue().loanNumber()));
        loanStatusColumn.setCellValueFactory(cell -> text(cell.getValue().status()));

        loanTable.setRowFactory(table -> {
            TableRow<LoanSummaryItem> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    navigator.showLoanDetails(row.getItem().loanNumber());
                }
            });
            return row;
        });

        addressesTab = new AddressesTab(addressesListController, customers, categories, tasks, this::checkReadiness);
    }

    /**
     * Loads the customer, or starts a blank form for a new one. Skipped while the
     * user is editing, so unsaved typing isn't lost when they come back to the
     * screen.
     */
    @Override
    public void onShow() {
        hideBanner();
        if (editing.get()) {
            return;
        }
        if (context.mode() == ScreenMode.NEW) {
            startNewCustomer();
        } else {
            loadCustomer();
        }
    }

    private void loadCustomer() {
        addLoanButton.setDisable(true);
        retryAction = this::loadCustomer;
        String number = context.customerNumber();
        tasks.run(() -> customers.get(number), loading,
                this::showCustomer, error -> showBanner(LOAD_FAILED));
    }

    private void loadLoans() {
        String number = customer.number();
        tasks.run(() -> customers.listLoans(number, 1, LOAN_PAGE_SIZE), loading,
                this::showLoans, error -> showLoansError());
        checkReadiness();
    }

    private void checkReadiness() {
        addLoanButton.setDisable(true);
        loanReadinessLabel.setText("Checking whether a loan can be added...");
        String number = customer.number();
        tasks.run(() -> customers.checkLoanReadiness(number), loading,
                this::showReadiness, error -> showLoansError());
    }

    private void showLoansError() {
        retryAction = this::loadLoans;
        showBanner(LOANS_FAILED);
    }

    private void showLoans(OperationResult<Page<LoanSummaryItem>> result) {
        if (!result.isSuccess()) {
            showLoansError();
            return;
        }
        loanTable.getItems().setAll(result.value().items());
    }

    private void showReadiness(OperationResult<ValidationReport> result) {
        if (!result.isSuccess()) {
            showLoansError();
            return;
        }
        ValidationReport report = result.value();
        if (report.errors().isEmpty()) {
            addLoanButton.setDisable(false);
            loanReadinessLabel.setText("");
        } else {
            addLoanButton.setDisable(true);
            loanReadinessLabel
                    .setText(String.join(" ", report.errors().stream().map(ValidationIssue::message).toList()));
        }
    }

    private void updateLinkButton() {
        linkCoborrowerButton.setVisible(navigator.isPickingCoBorrower() && customer != null);
    }

    private static ReadOnlyStringWrapper text(String value) {
        return new ReadOnlyStringWrapper(value == null ? "" : value);
    }

    private void showCustomer(OperationResult<Customer> result) {
        if (!result.isSuccess()) {
            showBanner(LOAD_FAILED);
            return;
        }
        customer = result.value();
        fillForm(customer);
    }

    private void fillForm(Customer loaded) {
        feedback.clear();
        String fullName = (loaded.title() + " " + loaded.firstName() + " " + loaded.familyName()).trim();
        customerNameLabel.setText(fullName);
        customerNumberLabel.setText(loaded.number());
        customerStatusLabel.setText(loaded.status());

        titleField.setText(loaded.title());
        firstNameField.setText(loaded.firstName());
        familyNameField.setText(loaded.familyName());
        nationalityField.setText(loaded.nationality());
        dobPicker.setValue(loaded.dateOfBirth());

        editing.set(false);

        for (Tab tab : detailsTabs.getTabs()) {
            tab.setDisable(false);
        }

        loadLoans();
        updateLinkButton();
        addressesTab.load(loaded.number());
    }

    private void startNewCustomer() {
        feedback.clear();
        customer = null;
        customerNameLabel.setText("New Customer");
        customerNumberLabel.setText("-");
        customerStatusLabel.setText("");

        titleField.clear();
        firstNameField.clear();
        familyNameField.clear();
        nationalityField.clear();
        dobPicker.setValue(null);

        editing.set(true);

        for (int i = 1; i < detailsTabs.getTabs().size(); i++) {
            detailsTabs.getTabs().get(i).setDisable(true);
        }

        loanTable.getItems().clear();
        addLoanButton.setDisable(true);
        loanReadinessLabel.setText("Save the customer before adding a loan.");
        updateLinkButton();

    }

    private void showBanner(String message) {
        saveErrorLabel.setText(message);
        saveErrorBanner.setVisible(true);
    }

    private void hideBanner() {
        saveErrorBanner.setVisible(false);
    }

    @FXML
    private void handleRetry() {
        hideBanner();
        retryAction.run();
    }

    @FXML
    private void handleEdit() {
        editing.set(true);
    }

    private CustomerInput readForm() {
        return new CustomerInput(firstNameField.getText().trim(), familyNameField.getText().trim(),
                dobPicker.getValue(), nationalityField.getText().trim(), titleField.getText().trim());
    }

    @FXML
    private void handleSave() {
        retryAction = this::handleSave;
        hideBanner();
        CustomerInput input = readForm();
        boolean creating = customer == null;
        String number = creating ? null : customer.number();
        tasks.run(() -> creating ? customers.create(input) : customers.update(number, input),
                saving, this::showSaveResult, error -> showBanner(SAVE_FAILED));
    }

    private void showSaveResult(OperationResult<SaveResult<Customer>> result) {
        if (!result.isSuccess()) {
            if (result.isRejected()) {
                showBanner(SAVE_REJECTED + result.errorMessage());
            } else {
                showBanner(SAVE_FAILED);
            }
            return;
        }
        SaveResult<Customer> saved = result.value();
        if (saved.value() == null) {
            feedback.showAllErrors(saved.validation());
            return;
        }

        feedback.clear();
        customer = saved.value();
        context = new CustomerScreenContext(ScreenMode.EXISTING, customer.number());
        fillForm(customer);
    }

    @FXML
    private void handleCancel() {
        feedback.clear();
        hideBanner();
        if (customer == null) {
            navigator.back();
        } else {
            fillForm(customer);
        }
    }

    @FXML
    private void handleAddLoan() {
        navigator.showNewLoan(customer.number());
    }

    @FXML
    private void handleLinkCoborrower() {
        navigator.completeCoBorrowerPick(customer.number());
    }
}