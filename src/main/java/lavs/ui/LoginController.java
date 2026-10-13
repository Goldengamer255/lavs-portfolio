package lavs.ui;

import java.util.concurrent.Executor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import lavs.gateway.OperationResult;
import lavs.service.AuthenticationService;
import lavs.service.SignedInUser;
import lavs.ui.support.TaskRunner;

// AI-assisted: initialize bindings and the background login call from Claude; all other methods written by me
/**
 * Controller for LoginView.fxml. Logs in MMM through the authentication service
 * and opens Customer Search on success.
 */
public final class LoginController {

    private static final String WRONG_LOGIN = "Incorrect username or password.";
    private static final String CANT_LOG_IN = "Couldn't log in. Check your username, password and connection, then try again.";

    private final AuthenticationService authentication;
    private final Navigator navigator;
    private final TaskRunner tasks;
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private ProgressIndicator loginSpinner;
    @FXML
    private Label errorBanner;

    /**
     * Creates the controller with the services it needs. The worker is the shared
     * background executor from the application shell.
     */
    public LoginController(AuthenticationService authentication, Navigator navigator, Executor worker) {
        this.authentication = authentication;
        this.navigator = navigator;
        this.tasks = new TaskRunner(worker);
    }

    @FXML
    private void initialize() {
        loginSpinner.visibleProperty().bind(busy);
        loginButton.disableProperty().bind(busy
                .or(usernameField.textProperty().isEmpty())
                .or(passwordField.textProperty().isEmpty()));
        usernameField.disableProperty().bind(busy);
        passwordField.disableProperty().bind(busy);
        errorBanner.managedProperty().bind(errorBanner.visibleProperty());
    }

    @FXML
    private void handleLogin() {
        hideError();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        tasks.run(() -> authentication.login(username, password), busy,
                this::showResult, error -> showError(CANT_LOG_IN));
    }

    private void showResult(OperationResult<SignedInUser> result) {
        if (result.isSuccess()) {
            passwordField.clear();
            navigator.showCustomerSearch();
        } else if (result.isRejected()) {
            passwordField.clear();
            showError(WRONG_LOGIN);
            passwordField.requestFocus();
        } else {
            showError(CANT_LOG_IN);
        }
    }

    private void showError(String message) {
        errorBanner.setText(message);
        errorBanner.setVisible(true);
    }

    private void hideError() {
        errorBanner.setText("");
        errorBanner.setVisible(false);
    }
}