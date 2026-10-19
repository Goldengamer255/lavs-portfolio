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
 * Displays loan status, repayment totals, and a paginated repayment schedule.
 *
 * <p>Service requests run through {@link TaskRunner}, with UI updates delivered
 * on the JavaFX application thread. Summary and schedule failures are tracked
 * separately so a successful response from one does not hide a failure in the other.</p>
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
     * Supplies the services and navigation used after FXML initialization.
     *
     * @param repayments service providing repayment totals and schedule pages
     * @param loans service providing the loan's current status
     * @param navigator navigation used to return to the previous screen
     * @param worker shared executor for background service requests
     */
    public LoanSummaryController(RepaymentService repayments, LoanService loans, Navigator navigator, Executor worker) {
        this.repayments = repayments;
        this.loans = loans;
        this.navigator = navigator;
        this.tasks = new TaskRunner(worker);
    }

    /**
     * Selects the loan to display and resets schedule navigation to the first page.
     *
     * @param context screen context containing the loan identifier
     */
    @Override
    public void setLoanContext(LoanScreenContext context) {
        this.loanNumber = context.loanNumber();
        this.page = 1;
    }

    @FXML
    private void initialize() {
        // All requests share busy state, keeping the spinner active and Retry
        // disabled until every outstanding task using that state has finished.
        loadingSpinner.visibleProperty().bind(busy);
        retryButton.visibleProperty().bind(errorLabel.visibleProperty());
        retryButton.disableProperty().bind(busy);
        // Collapse hidden error row so it doesn't leave a gap above the summary card.
        errorLabel.getParent().managedProperty().bind(errorLabel.visibleProperty());

        paymentNumberColumn.setCellValueFactory(cell -> text(String.valueOf(cell.getValue().paymentNumber())));
        dateColumn.setCellValueFactory(cell -> text(cell.getValue().date() == null
                ? ""
                : cell.getValue().date().format(DATE_FORMAT)));
        amountPaidColumn.setCellValueFactory(cell -> text(MONEY_FORMAT.format(cell.getValue().amountPaid())));
        principalColumn.setCellValueFactory(cell -> text(MONEY_FORMAT.format(cell.getValue().principalAmount())));
        interestColumn.setCellValueFactory(cell -> text(MONEY_FORMAT.format(cell.getValue().interestAmount())));
        remainingColumn.setCellValueFactory(cell -> text(MONEY_FORMAT.format(cell.getValue().amountRemaining())));
    }

    /**
     * Refreshes the summary, status, and current schedule page when shown or retried.
     * Must be called on the JavaFX application thread.
     */
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
        // Totals and status come from separate service calls but share the summary
        // error state; failure of either requires a summary retry.
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
        // Prevent additional page requests until this request completes.
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
        // Commit navigation only after a successful response, preserving the
        // displayed page and its controls if a page request fails.
        page = requested;

        scheduleTable.getItems().setAll(schedule.items());
        // Round up partial pages and retain "Page 1 of 1" for an empty schedule.
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
        // Retry all screen data while retaining the last successfully loaded page.
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
        // The single error banner prioritizes summary failures when both sections fail.
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
