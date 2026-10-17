package lavs.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * JavaFX implementation of the LAVS screen-navigation contract.
 *
 * <p>Each navigation creates an FXML view and controller, while Back restores the
 * previously created view. Restoring the same instance preserves screen state such as
 * search criteria and unsaved form values. Controllers that implement
 * {@link ScreenLifecycle} are notified whenever their screen becomes visible.</p>
 *
 * <p>A co-borrower search is handled as a temporary navigation mode. Its originating
 * loan view and history are restored before the selected customer is returned.</p>
 */
// AI-assisted: navigation history and co-borrower picker state implementation.
public final class FxmlNavigator implements Navigator {

    private static final String LOGIN = "/lavs/view/LoginView.fxml";
    private static final String CUSTOMER_SEARCH = "/lavs/view/CustomerSearchView.fxml";
    private static final String CUSTOMER_DETAILS = "/lavs/view/CustomerDetailsView.fxml";
    private static final String LOAN_DETAILS = "/lavs/view/LoanDetailsView.fxml";
    private static final String LOAN_SUMMARY = "/lavs/view/LoanSummaryView.fxml";
    private static final String STYLESHEET = "/lavs/css/lavs.css";

    private final Stage stage;
    private final Deque<ViewEntry> history = new ArrayDeque<>();
    private Callback<Class<?>, Object> controllerFactory;
    private ViewEntry current;
    private PickerState picker;

    /**
     * @param stage primary application stage whose scene is managed by this navigator
     */
    public FxmlNavigator(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "stage");
    }

    /**
     * Sets the controller factory used by every subsequently loaded FXML document.
     *
     * <p>The factory must be installed once by the application shell before the first
     * navigation.</p>
     *
     * @param controllerFactory application controller factory
     * @throws IllegalStateException if a factory has already been installed
     */
    public void setControllerFactory(Callback<Class<?>, Object> controllerFactory) {
        if (this.controllerFactory != null) {
            throw new IllegalStateException("Controller factory has already been set");
        }
        this.controllerFactory = Objects.requireNonNull(controllerFactory);
    }

    @Override
    public void showLogin() {
        // Login is a session boundary: authenticated screens must not remain reachable
        // through Back after logout.
        history.clear();
        picker = null;
        showNew(LOGIN, null, false);
    }

    @Override
    public void showCustomerSearch() {
        showNew(CUSTOMER_SEARCH, null, true);
    }

    @Override
    public void showNewCustomer() {
        showNew(CUSTOMER_DETAILS, new CustomerScreenContext(ScreenMode.NEW, null), true);
    }

    @Override
    public void showCustomerDetails(String customerNumber) {
        Objects.requireNonNull(customerNumber, "customerNumber");
        showNew(CUSTOMER_DETAILS,
                new CustomerScreenContext(ScreenMode.EXISTING, customerNumber), true);
    }

    @Override
    public void showNewLoan(String customerNumber) {
        Objects.requireNonNull(customerNumber, "customerNumber");
        showNew(LOAN_DETAILS,
                new LoanScreenContext(ScreenMode.NEW, customerNumber, null), true);
    }

    @Override
    public void showLoanDetails(String loanNumber) {
        Objects.requireNonNull(loanNumber, "loanNumber");
        showNew(LOAN_DETAILS,
                new LoanScreenContext(ScreenMode.EXISTING, null, loanNumber), true);
    }

    @Override
    public void showLoanSummary(String loanNumber) {
        Objects.requireNonNull(loanNumber, "loanNumber");
        showNew(LOAN_SUMMARY,
                new LoanScreenContext(ScreenMode.EXISTING, null, loanNumber), true);
    }

    @Override
    public void pickCoBorrower(String loanNumber, Consumer<String> onSelected) {
        Objects.requireNonNull(loanNumber, "loanNumber");
        Objects.requireNonNull(onSelected, "onSelected");
        if (picker != null) {
            throw new IllegalStateException("A co-borrower selection is already active");
        }
        if (current == null) {
            throw new IllegalStateException("No originating loan screen is active");
        }

        // Snapshot the route as well as the origin so cancelling and completing the
        // picker both return to exactly the same loan workflow.
        picker = new PickerState(current, List.copyOf(history), onSelected);
        showNew(CUSTOMER_SEARCH, null, true);
    }

    @Override
    public boolean isPickingCoBorrower() {
        return picker != null;
    }

    @Override
    public void completeCoBorrowerPick(String customerNumber) {
        Objects.requireNonNull(customerNumber, "customerNumber");
        if (picker == null) {
            throw new IllegalStateException("No co-borrower selection is active");
        }

        PickerState completed = picker;
        picker = null;
        // Restore the loan before invoking the callback so any resulting UI update is
        // applied to the visible originating controller.
        restorePickerOrigin(completed);
        completed.onSelected().accept(customerNumber);
    }

    @Override
    public void back() {
        if (picker != null) {
            PickerState cancelled = picker;
            picker = null;
            restorePickerOrigin(cancelled);
            return;
        }
        if (!history.isEmpty()) {
            showExisting(history.pop());
        }
    }

    private void restorePickerOrigin(PickerState state) {
        history.clear();
        history.addAll(state.previousHistory());
        showExisting(state.origin());
    }

    /**
     * Loads a fresh view and optionally records the current view for Back navigation.
     */
    private void showNew(String resourcePath, Object context, boolean rememberCurrent) {
        ensureReady();
        if (rememberCurrent && current != null) {
            history.push(current);
        }

        URL resource = FxmlNavigator.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalArgumentException("FXML resource not found: " + resourcePath);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        loader.setControllerFactory(controllerFactory);
        try {
            Parent root = loader.load();
            ViewEntry entry = new ViewEntry(root, loader.getController(), context);
            applyContext(entry.controller(), context);
            showExisting(entry);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load " + resourcePath, exception);
        }
    }

    /** Displays an already loaded view and notifies its controller of activation. */
    private void showExisting(ViewEntry entry) {
        current = entry;
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(entry.root(), 1000, 700);
            URL stylesheet = FxmlNavigator.class.getResource(STYLESHEET);
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet.toExternalForm());
            }
            stage.setScene(scene);
        } else {
            scene.setRoot(entry.root());
        }

        if (entry.controller() instanceof ScreenLifecycle lifecycle) {
            lifecycle.onShow();
        }
    }

    /**
     * Supplies route parameters before the controller's lifecycle callback runs.
     */
    private void applyContext(Object controller, Object context) {
        if (context instanceof CustomerScreenContext customerContext
                && controller instanceof CustomerContextAware contextAware) {
            contextAware.setCustomerContext(customerContext);
        } else if (context instanceof LoanScreenContext loanContext
                && controller instanceof LoanContextAware contextAware) {
            contextAware.setLoanContext(loanContext);
        }
    }

    private void ensureReady() {
        if (controllerFactory == null) {
            throw new IllegalStateException("Controller factory has not been set");
        }
    }

    /** Loaded view and its controller state, retained as one navigation-history entry. */
    private record ViewEntry(Parent root, Object controller, Object context) {}

    /** Snapshot of the route suspended while the user selects a co-borrower. */
    private record PickerState(
            ViewEntry origin,
            List<ViewEntry> previousHistory,
            Consumer<String> onSelected) {
        private PickerState {
            previousHistory = new ArrayList<>(previousHistory);
        }
    }
}
