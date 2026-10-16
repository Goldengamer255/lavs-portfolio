package lavs.ui;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Executor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lavs.domain.Loan;
import lavs.domain.LoanSummary;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;
import lavs.gateway.OperationResult;
import lavs.service.LoanService;
import lavs.service.RepaymentService;
import lavs.ui.support.TaskRunner;

// AI-assisted: paging, loading and retry logic from Claude; the rest written by me
/**
 * - Loan summary screen controller: shows loan totals + paged repayment
 * schedule.
 */
public final class LoanSummaryController implements LoanContextAware, ScreenLifecycle {

    private static final int PAGE_SIZE = 20;
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-NZ"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SUMMARY_FAILED = "Couldn't load the loan summary. Check your connection and try again.";
    private static final String SCHEDULE_FAILED = "Couldn't load the repayment schedule. Check your connection and try again.";

    private final RepaymentService repayments;
    private final LoanService loans;
    private final Navigator navigator;
    private final TaskRunner tasks;
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    private String loanNumber;
    private long page = 1;
    private long totalPages = 1;
    private boolean summaryFailed = false;
    private boolean scheduleFailed = false;

    @FXML
    private Label titleLabel;
    @FXML
    private ProgressIndicator loadingSpinner;
    @FXML
    private Label statusLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private Button retryButton;
    @FXML
    private Label customerValue;
    @FXML
    private Label finalPaymentAmountValue;
    @FXML
    private Label paymentCountValue;
    @FXML
    private Label totalInterestValue;
    @FXML
    private Label finalPaymentDateValue;
    @FXML
    private Label totalPaidValue;
    @FXML
    private TableView<RepaymentScheduleItem> scheduleTable;
    @FXML
    private TableColumn<RepaymentScheduleItem, String> paymentNumberColumn;
    @FXML
    private TableColumn<RepaymentScheduleItem, String> dateColumn;
    @FXML
    private TableColumn<RepaymentScheduleItem, String> amountPaidColumn;
    @FXML
    private TableColumn<RepaymentScheduleItem, String> principalColumn;
    @FXML
    private TableColumn<RepaymentScheduleItem, String> interestColumn;
    @FXML
    private TableColumn<RepaymentScheduleItem, String> remainingColumn;
    @FXML
    private Button backButton;
    @FXML
    private Button prevPageButton;
    @FXML
    private Label pageLabel;
    @FXML
    private Button nextPageButton;

    /**
     * - Creates the controller with the services it needs. The worker is the shared
     * - background executor from the application shell.
     */
    public LoanSummaryController(RepaymentService repayments, LoanService loans, Navigator navigator, Executor worker) {
        this.repayments = repayments;
        this.loans = loans;
        this.navigator = navigator;
        this.tasks = new TaskRunner(worker);
    }

    @Override
    public void setLoanContext(LoanScreenContext context) {
        this.loanNumber = context.loanNumber();
        this.page = 1;
    }

    @FXML
    private void initialize() {
        loadingSpinner.visibleProperty().bind(busy);
        retryButton.visibleProperty().bind(errorLabel.visibleProperty());
        retryButton.disableProperty().bind(busy);

        paymentNumberColumn.setCellValueFactory(cell -> text(String.valueOf(cell.getValue().paymentNumber())));
        dateColumn.setCellValueFactory(cell -> text(cell.getValue().date() == null
                ? ""
                : cell.getValue().date().format(DATE_FORMAT)));
        amountPaidColumn.setCellValueFactory(cell -> text(MONEY_FORMAT.format(cell.getValue().amountPaid())));
        principalColumn.setCellValueFactory(cell -> text(MONEY_FORMAT.format(cell.getValue().principalAmount())));
        interestColumn.setCellValueFactory(cell -> text(MONEY_FORMAT.format(cell.getValue().interestAmount())));
        remainingColumn.setCellValueFactory(cell -> text(MONEY_FORMAT.format(cell.getValue().amountRemaining())));
    }

    @Override
    public void onShow() {
        titleLabel.setText("Loan " + loanNumber + " Summary");
        statusLabel.setText("Status: -");
        summaryFailed = false;
        scheduleFailed = false;
        updateErrorDisplay();
        loadSummary();
        loadPage(page);
    }

    private void loadSummary() {
        String number = loanNumber;
        tasks.run(() -> repayments.getSummary(number), busy, this::showSummary,
                error -> {
                    summaryFailed = true;
                    updateErrorDisplay();
                });
        tasks.run(() -> loans.get(number), busy, this::showStatus,
                error -> {
                    summaryFailed = true;
                    updateErrorDisplay();
                });
    }

    private void showStatus(OperationResult<Loan> result) {
        if (result.isSuccess()) {
            statusLabel.setText("Status: " + result.value().status());
        } else {
            summaryFailed = true;
            updateErrorDisplay();
        }
    }

    private void showSummary(OperationResult<LoanSummary> result) {
        if (!result.isSuccess()) {
            summaryFailed = true;
            updateErrorDisplay();
            return;
        }
        LoanSummary summary = result.value();
        customerValue.setText(summary.customerName());
        finalPaymentAmountValue.setText(MONEY_FORMAT.format(summary.finalPaymentAmount()));
        paymentCountValue.setText(String.valueOf(summary.numberOfPayments()));
        totalInterestValue.setText(MONEY_FORMAT.format(summary.totalInterestPaid()));
        finalPaymentDateValue
                .setText(summary.finalPaymentDate() == null ? "" : summary.finalPaymentDate().format(DATE_FORMAT));
        totalPaidValue.setText(MONEY_FORMAT.format(summary.totalAmountPaid()));
    }

    private void loadPage(long requested) {
        String number = loanNumber;
        prevPageButton.setDisable(true);
        nextPageButton.setDisable(true);
        tasks.run(() -> repayments.getSchedule(number, requested, PAGE_SIZE), busy,
                result -> showPage(result, requested),
                error -> {
                    scheduleFailed = true;
                    updateErrorDisplay();
                    updatePager();
                });
    }

    private void showPage(OperationResult<Page<RepaymentScheduleItem>> result, long requested) {
        if (!result.isSuccess()) {
            scheduleFailed = true;
            updateErrorDisplay();
            updatePager();
            return;
        }

        scheduleFailed = false;
        updateErrorDisplay();

        Page<RepaymentScheduleItem> schedule = result.value();
        page = requested;

        scheduleTable.getItems().setAll(schedule.items());
        totalPages = Math.max(1, (schedule.total() + PAGE_SIZE - 1) / PAGE_SIZE);
        updatePager();
    }

    private void updatePager() {
        pageLabel.setText("Page " + page + " of " + totalPages);
        prevPageButton.setDisable(page <= 1);
        nextPageButton.setDisable(page >= totalPages);
    }

    @FXML
    private void handleBack() {
        navigator.back();
    }

    @FXML
    private void handleRetry() {
        onShow();
    }

    @FXML
    private void handlePreviousPage() {
        loadPage(page - 1);
    }

    @FXML
    private void handleNextPage() {
        loadPage(page + 1);
    }

    private void updateErrorDisplay() {
        if (summaryFailed) {
            errorLabel.setText(SUMMARY_FAILED);
            errorLabel.setVisible(true);
        } else if (scheduleFailed) {
            errorLabel.setText(SCHEDULE_FAILED);
            errorLabel.setVisible(true);
        } else {
            errorLabel.setVisible(false);
        }
    }

    private static ReadOnlyStringWrapper text(String value) {
        return new ReadOnlyStringWrapper(value == null ? "" : value);
    }
}