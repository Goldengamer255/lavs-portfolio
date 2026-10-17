package lavs.service;

import java.util.Objects;
import java.util.Optional;
import lavs.gateway.AuthenticationGateway;
import lavs.gateway.OperationResult;

/**
 * Coordinates authentication and exposes the signed-in user to the rest of the
 * application without leaking gateway-specific types.
 *
 * <p>The cached user is held only for the lifetime of the application process. The
 * gateway's connection remains responsible for the actual authenticated mainframe
 * session.</p>
 */
public final class DefaultAuthenticationService implements AuthenticationService {

    private final AuthenticationGateway gateway;
    private volatile SignedInUser currentUser;

    /**
     * @param gateway authentication persistence boundary
     */
    public DefaultAuthenticationService(AuthenticationGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    @Override
    public OperationResult<SignedInUser> login(String username, String password) {
        OperationResult<Boolean> result = gateway.login(username, password);
        if (!result.isSuccess()) {
            return OperationResult.failure(
                    result.errorCode(), result.errorMessage(), result.transactionId());
        }

        // The login endpoint does not return a separate display name, so the
        // normalized username is used for both values until profile data is available.
        String normalizedUsername = username.trim();
        SignedInUser signedIn = new SignedInUser(normalizedUsername, normalizedUsername);
        currentUser = signedIn;
        return OperationResult.success(signedIn, result.transactionId());
    }

    @Override
    public OperationResult<Boolean> logout() {
        OperationResult<Boolean> result = gateway.logout();
        if (result.isSuccess()) {
            // Clear local state only after the gateway confirms that its session ended.
            currentUser = null;
        }
        return result;
    }

    @Override
    public Optional<SignedInUser> currentUser() {
        return Optional.ofNullable(currentUser);
    }
}
