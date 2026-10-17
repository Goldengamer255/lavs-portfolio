package lavs.adapter;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;

/**
 * Restores a mainframe session once when another login has invalidated this
 * client's transaction number.
 *
 * <p>The mainframe keeps one transaction sequence per user. If the same account
 * logs in elsewhere, this connection can receive an invalid transaction number.
 * After a successful login, this wrapper retains the credentials in memory and
 * uses them to log in again and resend the failed request exactly once.</p>
 */
// AI-assisted: connection wrapper structure and one-retry recovery flow.
public final class RecoveringConnection implements Connection {

    private static final String INVALID_TRANSACTION = "invalid transaction number";

    private final Connection delegate;
    private String username;
    private String password;

    public RecoveringConnection(Connection delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public synchronized MessageResponse send(MessageRequest request) {
        MessageResponse response = delegate.send(request);
        if (!isInvalidTransaction(response) || username == null || password == null) {
            return response;
        }

        try {
            Status login = delegate.Login(username, password);
            if (login.getWasSuccessful()) {
                // Call the delegate directly so a second failure is returned rather
                // than starting another recovery attempt.
                return delegate.send(request);
            }
        } catch (IOException ignored) {
            // Preserve the original server response when session recovery fails.
        }
        return response;
    }

    @Override
    public synchronized Status Login(String username, String password) throws IOException {
        Status status = delegate.Login(username, password);
        if (status.getWasSuccessful()) {
            this.username = Objects.requireNonNull(username, "username");
            this.password = Objects.requireNonNull(password, "password");
        }
        return status;
    }

    @Override
    public synchronized void Logout() {
        delegate.Logout();
        username = null;
        password = null;
    }

    @Override
    public synchronized boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public synchronized void close() throws IOException {
        username = null;
        password = null;
        delegate.close();
    }

    private boolean isInvalidTransaction(MessageResponse response) {
        if (response == null || response.getStatus() == null) {
            return false;
        }
        String message = response.getStatus().getErrorMessage();
        return message != null
                && message.toLowerCase(Locale.ROOT).contains(INVALID_TRANSACTION);
    }
}
