package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import mainframe.client.StubConnection;
import org.junit.jupiter.api.Test;

/** Verifies translation between the supplied mainframe API and the gateway contract. */
class MainframeAuthenticationGatewayTest {

    @Test
    void constructorRejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> new MainframeAuthenticationGateway(null));
    }

    @Test
    void mapsSuccessfulLoginStatus() {
        // StubConnection exercises the real Status type without making a network call.
        StubConnection connection = new StubConnection();
        connection.setupSuccessfulLoginResult("token", "user", 12);
        MainframeAuthenticationGateway gateway = new MainframeAuthenticationGateway(connection);

        OperationResult<Boolean> result = gateway.login("user", "password");

        assertTrue(result.isSuccess());
        assertEquals(Boolean.TRUE, result.value());
        assertEquals(1, result.transactionId());
    }

    @Test
    void mapsRejectedLoginStatus() {
        // Preserve the server response so controllers can distinguish rejection from
        // an unavailable server and display the appropriate feedback.
        StubConnection connection = new StubConnection();
        connection.setupFailedLoginResult(
                StatusMessage.SERVER_FAILURE.getCode(), "Invalid credentials");
        MainframeAuthenticationGateway gateway = new MainframeAuthenticationGateway(connection);

        OperationResult<Boolean> result = gateway.login("user", "wrong");

        assertFalse(result.isSuccess());
        assertTrue(result.isRejected());
        assertEquals("Invalid credentials", result.errorMessage());
        assertEquals(1, result.transactionId());
    }

    @Test
    void mapsTransportFailureToUnavailableResult() {
        MainframeAuthenticationGateway gateway = new MainframeAuthenticationGateway(
                new AuthenticationConnection(new IOException("Network down")));

        OperationResult<Boolean> result = gateway.login("user", "password");

        assertTrue(result.isUnavailable());
        assertEquals("Network down", result.errorMessage());
    }

    @Test
    void logoutDelegatesAndReturnsSuccess() {
        AuthenticationConnection connection = new AuthenticationConnection(null);
        MainframeAuthenticationGateway gateway = new MainframeAuthenticationGateway(connection);

        OperationResult<Boolean> result = gateway.logout();

        assertTrue(result.isSuccess());
        assertTrue(connection.loggedOut);
    }

    private static final class AuthenticationConnection implements Connection {
        private final IOException loginFailure;
        private boolean loggedOut;

        private AuthenticationConnection(IOException loginFailure) {
            this.loginFailure = loginFailure;
        }

        @Override public Status Login(String username, String password) throws IOException {
            if (loginFailure != null) throw loginFailure;
            return new Status(1);
        }
        @Override public void Logout() { loggedOut = true; }
        @Override public boolean isConnected() { return true; }
        @Override public MessageResponse send(MessageRequest request) {
            throw new UnsupportedOperationException();
        }
        @Override public void close() {}
    }
}
