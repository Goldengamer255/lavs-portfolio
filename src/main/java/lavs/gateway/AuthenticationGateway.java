package lavs.gateway;

/**
 * Provides authentication operations for the shared mainframe session.
 * Results expose application-level status details without mainframe client types.
 */
public interface AuthenticationGateway {
    /**
     * Authenticates using the supplied credentials.
     *
     * @return true on success; failures carry error details and a null value
     */
    OperationResult<Boolean> login(String username, String password);

    /** Ends the current session and returns an acknowledgement on success. */
    OperationResult<Boolean> logout();
}
