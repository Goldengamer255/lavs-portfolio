package lavs.adapter;

import java.io.IOException;
import java.util.Objects;
import lavs.gateway.AuthenticationGateway;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.Status;

/**
 * Adapts the supplied mainframe {@link Connection} authentication API to the
 * application-owned {@link AuthenticationGateway} contract.
 *
 * <p>This boundary keeps mainframe response types and transport exceptions out of
 * the service and UI layers.</p>
 */
public final class MainframeAuthenticationGateway implements AuthenticationGateway {

    private final Connection connection;

    /**
     * Creates an authentication gateway that shares the application's mainframe
     * connection and its authenticated session.
     *
     * @param connection mainframe connection used for login and logout
     */
    public MainframeAuthenticationGateway(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public OperationResult<Boolean> login(String username, String password) {
        try {
            return fromStatus(connection.Login(username, password));
        } catch (IOException exception) {
            // Error code 10 is the shared contract for an unavailable mainframe.
            return OperationResult.failure(10, exception.getMessage(), 0);
        }
    }

    @Override
    public OperationResult<Boolean> logout() {
        connection.Logout();
        return OperationResult.success(true, 0);
    }

    /** Converts the mainframe status without losing its diagnostic transaction ID. */
    private OperationResult<Boolean> fromStatus(Status status) {
        if (status.getWasSuccessful()) {
            return OperationResult.success(true, status.getTransactionId());
        }
        return OperationResult.failure(
                status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
    }
}
