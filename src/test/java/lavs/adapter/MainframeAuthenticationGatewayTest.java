package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lavs.gateway.OperationResult;
import mainframe.client.StatusMessage;
import mainframe.client.StubConnection;
import org.junit.jupiter.api.Test;

/** Verifies translation between the supplied mainframe API and the gateway contract. */
class MainframeAuthenticationGatewayTest {

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
}
