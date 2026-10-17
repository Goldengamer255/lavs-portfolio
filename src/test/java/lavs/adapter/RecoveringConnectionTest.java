package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import org.junit.jupiter.api.Test;

class RecoveringConnectionTest {

    private static final MessageRequest REQUEST =
            new MessageRequest("customer", MessageRequest.QUERY);

    @Test
    void returnsSuccessfulResponseWithoutLoggingInAgain() throws IOException {
        FakeConnection delegate = new FakeConnection();
        RecoveringConnection connection = loggedIn(delegate);
        MessageResponse expected = success(2);
        delegate.responses.add(expected);

        MessageResponse actual = connection.send(REQUEST);

        assertSame(expected, actual);
        assertEquals(1, delegate.loginCalls);
        assertEquals(1, delegate.sendCalls);
    }

    @Test
    void logsInAgainAndRetriesAnInvalidTransactionOnce() throws IOException {
        FakeConnection delegate = new FakeConnection();
        RecoveringConnection connection = loggedIn(delegate);
        delegate.responses.add(invalidTransaction(2));
        MessageResponse expected = success(8);
        delegate.responses.add(expected);

        MessageResponse actual = connection.send(REQUEST);

        assertSame(expected, actual);
        assertEquals(2, delegate.loginCalls);
        assertEquals(2, delegate.sendCalls);
        assertEquals("Group_4", delegate.lastUsername);
        assertEquals("build", delegate.lastPassword);
    }

    @Test
    void doesNotRetryASecondInvalidTransaction() throws IOException {
        FakeConnection delegate = new FakeConnection();
        RecoveringConnection connection = loggedIn(delegate);
        delegate.responses.add(invalidTransaction(2));
        MessageResponse secondFailure = invalidTransaction(8);
        delegate.responses.add(secondFailure);

        MessageResponse actual = connection.send(REQUEST);

        assertSame(secondFailure, actual);
        assertEquals(2, delegate.loginCalls);
        assertEquals(2, delegate.sendCalls);
    }

    @Test
    void preservesOriginalFailureWhenLoginRecoveryFails() throws IOException {
        FakeConnection delegate = new FakeConnection();
        RecoveringConnection connection = loggedIn(delegate);
        MessageResponse original = invalidTransaction(2);
        delegate.responses.add(original);
        delegate.loginStatus = new Status(3, "Login rejected", 3);

        MessageResponse actual = connection.send(REQUEST);

        assertSame(original, actual);
        assertEquals(2, delegate.loginCalls);
        assertEquals(1, delegate.sendCalls);
    }

    @Test
    void logoutClearsCredentialsUsedForRecovery() throws IOException {
        FakeConnection delegate = new FakeConnection();
        RecoveringConnection connection = loggedIn(delegate);
        connection.Logout();
        MessageResponse original = invalidTransaction(2);
        delegate.responses.add(original);

        MessageResponse actual = connection.send(REQUEST);

        assertSame(original, actual);
        assertEquals(1, delegate.loginCalls);
        assertEquals(1, delegate.sendCalls);
        assertEquals(1, delegate.logoutCalls);
    }

    private RecoveringConnection loggedIn(FakeConnection delegate) throws IOException {
        RecoveringConnection connection = new RecoveringConnection(delegate);
        connection.Login("Group_4", "build");
        return connection;
    }

    private static MessageResponse success(long transactionId) {
        return new MessageResponse(new Status(transactionId), new HashMap<>());
    }

    private static MessageResponse invalidTransaction(long transactionId) {
        return new MessageResponse(
                new Status(3, "Invalid transaction number", transactionId),
                new HashMap<>());
    }

    private static final class FakeConnection implements Connection {
        private final Queue<MessageResponse> responses = new ArrayDeque<>();
        private Status loginStatus = new Status(1);
        private int sendCalls;
        private int loginCalls;
        private int logoutCalls;
        private String lastUsername;
        private String lastPassword;

        @Override
        public MessageResponse send(MessageRequest request) {
            sendCalls++;
            return responses.remove();
        }

        @Override
        public Status Login(String username, String password) {
            loginCalls++;
            lastUsername = username;
            lastPassword = password;
            return loginStatus;
        }

        @Override
        public void Logout() {
            logoutCalls++;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void close() {
        }
    }
}
