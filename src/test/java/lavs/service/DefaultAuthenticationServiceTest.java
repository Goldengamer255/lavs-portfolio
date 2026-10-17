package lavs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lavs.gateway.AuthenticationGateway;
import lavs.gateway.OperationResult;
import org.junit.jupiter.api.Test;

/** Tests authentication state management independently of the mainframe transport. */
class DefaultAuthenticationServiceTest {

    @Test
    void successfulLoginReturnsAndCachesUser() {
        AuthenticationGateway gateway = new FakeAuthenticationGateway(
                OperationResult.success(true, 42));
        DefaultAuthenticationService service = new DefaultAuthenticationService(gateway);

        OperationResult<SignedInUser> result = service.login(" hargun ", "password");

        assertTrue(result.isSuccess());
        assertEquals(new SignedInUser("hargun", "hargun"), result.value());
        assertEquals(result.value(), service.currentUser().orElseThrow());
        assertEquals(42, result.transactionId());
    }

    @Test
    void failedLoginPreservesGatewayFailureAndDoesNotCacheUser() {
        AuthenticationGateway gateway = new FakeAuthenticationGateway(
                OperationResult.failure(3, "Rejected", 8));
        DefaultAuthenticationService service = new DefaultAuthenticationService(gateway);

        OperationResult<SignedInUser> result = service.login("user", "wrong");

        assertFalse(result.isSuccess());
        assertEquals(3, result.errorCode());
        assertEquals("Rejected", result.errorMessage());
        assertEquals(8, result.transactionId());
        assertTrue(service.currentUser().isEmpty());
    }

    @Test
    void successfulLogoutClearsCachedUser() {
        FakeAuthenticationGateway gateway = new FakeAuthenticationGateway(
                OperationResult.success(true, 1));
        DefaultAuthenticationService service = new DefaultAuthenticationService(gateway);
        service.login("user", "password");

        OperationResult<Boolean> result = service.logout();

        assertTrue(result.isSuccess());
        assertTrue(service.currentUser().isEmpty());
    }

    /** Deterministic gateway used to test service behavior without network I/O. */
    private static final class FakeAuthenticationGateway implements AuthenticationGateway {
        private final OperationResult<Boolean> loginResult;

        private FakeAuthenticationGateway(OperationResult<Boolean> loginResult) {
            this.loginResult = loginResult;
        }

        @Override
        public OperationResult<Boolean> login(String username, String password) {
            return loginResult;
        }

        @Override
        public OperationResult<Boolean> logout() {
            return OperationResult.success(true, 2);
        }
    }
}
