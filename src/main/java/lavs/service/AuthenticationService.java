package lavs.service;

import lavs.gateway.OperationResult;
import java.util.Optional;

/** Session operations; network work runs on the shared worker, not the UI thread. */
// AI-assisted: service contract scaffolding for team review.
public interface AuthenticationService {
    OperationResult<SignedInUser> login(String username, String password);
    OperationResult<Boolean> logout();

    /** Cached user, empty before login and after logout; does not perform I/O. */
    Optional<SignedInUser> currentUser();
}
