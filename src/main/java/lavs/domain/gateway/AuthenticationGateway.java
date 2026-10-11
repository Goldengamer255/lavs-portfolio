package lavs.domain.gateway;

public interface AuthenticationGateway {
    OperationResult<Boolean> login(String username, String password);
}
