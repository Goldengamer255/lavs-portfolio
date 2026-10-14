package lavs.ui;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import lavs.domain.CustomerSummary;
import lavs.domain.Page;
import lavs.gateway.OperationResult;
import lavs.service.CustomerService;
import lavs.ui.support.TaskRunner;

// AI-assisted: Primarily written by me; the row-click handler and background search call came from Claude
/**
 * Controller for CustomerSearchView.fxml. Searches customers by name and opens
 * chosen customer, or a blank one. Also used to pick a co-borrower for a loan.
 */
public final class CustomerSearchController implements ScreenLifecycle {

    private static final int PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ERROR_STYLE = "field-error";
    private static final String SEARCH_FAILED = "Couldn't search right now. Check your connection and try again.";

    private final CustomerService customers;
    private final Navigator navigator;
    private final TaskRunner tasks;
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private String lastSearch;

    @FXML
    private Label titleLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;
    @FXML
    private ProgressIndicator searchSpinner;
    @FXML
    private TableView<CustomerSummary> customerTable;
    @FXML
    private TableColumn<CustomerSummary, String> numberColumn;
    @FXML
    private TableColumn<CustomerSummary, String> nameColumn;
    @FXML
    private TableColumn<CustomerSummary, String> titleColumn;
    @FXML
    private TableColumn<CustomerSummary, String> dobColumn;
    @FXML
    private TableColumn<CustomerSummary, String> statusColumn;
    @FXML
    private Label resultCountLabel;

    /**
     * Creates the controller with the services it needs. The worker is the shared
     * background executor from the application shell.
     */
    public CustomerSearchController(CustomerService customers, Navigator navigator, Executor worker) {
        this.customers = customers;
        this.navigator = navigator;
        this.tasks = new TaskRunner(worker);
    }

    @FXML
    private void initialize() {
        searchSpinner.visibleProperty().bind(busy);
        searchButton.disableProperty().bind(busy);

        numberColumn.setCellValueFactory(cell -> text(cell.getValue().number()));
        nameColumn.setCellValueFactory(cell -> text(cell.getValue().firstName() + " " + cell.getValue().familyName()));
        titleColumn.setCellValueFactory(cell -> text(cell.getValue().title()));
        statusColumn.setCellValueFactory(cell -> text(cell.getValue().status()));
        dobColumn.setCellValueFactory(cell -> text(cell.getValue().dateOfBirth() == null
                ? ""
                : cell.getValue().dateOfBirth().format(DATE_FORMAT)));

        customerTable.setRowFactory(table -> {
            TableRow<CustomerSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    navigator.showCustomerDetails(row.getItem().number());
                }
            });
            return row;
        });
    }

    /**
     * Updates the title for pick mode and re-runs the last search, so changes made
     * on other screens show up when the user comes back.
     */
    @Override
    public void onShow() {
        if (navigator.isPickingCoBorrower()) {
            titleLabel.setText("Choose a co-borrower");
        } else {
            titleLabel.setText("Customers");
        }

        if (lastSearch != null) {
            runSearch();
        }
    }

    @FXML
    private void handleSearch() {
        lastSearch = searchField.getText().trim();
        runSearch();
    }

    @FXML
    private void handleAddCustomer() {
        navigator.showNewCustomer();
    }

    private void runSearch() {
        String query = lastSearch;
        tasks.run(() -> customers.search(query, 1, PAGE_SIZE), busy,
                this::showResults, error -> showError(SEARCH_FAILED));
    }

    private void showResults(OperationResult<Page<CustomerSummary>> result) {
        if (!result.isSuccess()) {
            showError(SEARCH_FAILED);
            return;
        }

        Page<CustomerSummary> page = result.value();
        customerTable.getItems().setAll(page.items());

        int returnedCount = page.items().size();
        long totalCount = page.total();

        if (totalCount == 0) {
            showMessage("No customers match \"" + lastSearch + "\".");
        } else if (returnedCount < totalCount) {
            showMessage("Showing " + returnedCount + " of " + totalCount
                    + " matching customers. Refine your search to narrow it down.");
        } else {
            showMessage(totalCount + (totalCount == 1 ? " matching customer." : " matching customers."));
        }
    }

    private void showMessage(String message) {
        resultCountLabel.setText(message);
        resultCountLabel.getStyleClass().remove(ERROR_STYLE);
    }

    private void showError(String message) {
        resultCountLabel.setText(message);
        if (!resultCountLabel.getStyleClass().contains(ERROR_STYLE)) {
            resultCountLabel.getStyleClass().add(ERROR_STYLE);
        }
    }

    private static ReadOnlyStringWrapper text(String value) {
        return new ReadOnlyStringWrapper(value == null ? "" : value);
    }
}