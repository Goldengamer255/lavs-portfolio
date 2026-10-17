package lavs.ui;

import java.util.Objects;
import java.util.concurrent.Executor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lavs.gateway.OperationResult;
import lavs.service.AuthenticationService;
import lavs.service.SignedInUser;
import lavs.ui.support.Dialogs;
import lavs.ui.support.TaskRunner;

/** Header shown above every authenticated screen. */
// AI-assisted: shared header layout, connection-state listener, and logout flow.
final class SessionHeader extends VBox {

    private static final String LOGOUT_FAILED = "Couldn't log out. Please try again.";

    private final AuthenticationService authentication;
    private final Navigator navigator;
    private final TaskRunner tasks;
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final Label connectionLabel = new Label();
    private final Label errorLabel = new Label();
    private final ChangeListener<Boolean> connectionListener =
            (observable, previous, connected) -> updateConnectionStatus(connected);

    SessionHeader(AuthenticationService authentication, Navigator navigator,
            ReadOnlyBooleanProperty online, Executor worker) {
        this.authentication = Objects.requireNonNull(authentication, "authentication");
        this.navigator = Objects.requireNonNull(navigator, "navigator");
        this.tasks = new TaskRunner(Objects.requireNonNull(worker, "worker"));
        Objects.requireNonNull(online, "online");

        getStyleClass().add("session-header-container");
        setPadding(new Insets(10, 24, 10, 24));

        Label applicationLabel = new Label("LAVS");
        applicationLabel.getStyleClass().add("session-header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        SignedInUser user = authentication.currentUser().orElse(null);
        Label userLabel = new Label(user == null ? "Signed in" : user.displayName());
        userLabel.getStyleClass().add("session-user");

        connectionLabel.getStyleClass().add("connection-status");
        updateConnectionStatus(online.get());
        online.addListener(new WeakChangeListener<>(connectionListener));

        Button logoutButton = new Button("Log Out");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.disableProperty().bind(busy);
        logoutButton.setOnAction(event -> logout());

        HBox content = new HBox(12, applicationLabel, spacer,
                connectionLabel, userLabel, logoutButton);
        content.setAlignment(Pos.CENTER_LEFT);

        errorLabel.getStyleClass().add("field-error");
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        setSpacing(4);
        getChildren().addAll(content, errorLabel);
    }

    private void logout() {
        if (!Dialogs.confirmLogout(getScene().getWindow())) {
            return;
        }
        errorLabel.setVisible(false);
        tasks.run(authentication::logout, busy, this::showLogoutResult,
                error -> showError());
    }

    private void showLogoutResult(OperationResult<Boolean> result) {
        if (result.isSuccess()) {
            navigator.showLogin();
        } else {
            showError();
        }
    }

    private void showError() {
        errorLabel.setText(LOGOUT_FAILED);
        errorLabel.setVisible(true);
    }

    private void updateConnectionStatus(boolean online) {
        connectionLabel.setText(online ? "● Online" : "● Offline");
        connectionLabel.getStyleClass().removeAll("connection-online", "connection-offline");
        connectionLabel.getStyleClass().add(
                online ? "connection-online" : "connection-offline");
    }
}
