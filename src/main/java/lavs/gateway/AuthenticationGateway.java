package lavs.gateway;

public interface AuthenticationGateway {
    // this should return true if login is successful, false otherwise. The errorCode and errorMessage should provide more information in case of failure.
    OperationResult<Boolean> login(String username, String password);

    // same as the login
    OperationResult<Boolean> logout();
}
