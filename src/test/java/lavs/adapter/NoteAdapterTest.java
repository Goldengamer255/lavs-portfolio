package lavs.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import lavs.domain.Note;
import lavs.domain.NoteSummary;
import lavs.domain.Page;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Test;

/**
 * Checks the note messages sent and how replies are read, without a real
 * mainframe.
 */
class NoteAdapterTest {

    private final FakeConnection connection = new FakeConnection();
    private final NoteAdapter adapter = new NoteAdapter(connection);

    @Test
    void deleteReturnsTrue() {
        connection.reply(success(15, "customer", "C1", "id", "8"));

        OperationResult<Boolean> result = adapter.deleteNote("C1", 8);

        assertTrue(result.value());
        assertEquals("customer.deleteNote", connection.sent.get(0).getRequestType());
    }

    @Test
    void retrieveReturnsTheFullNote() {
        connection.reply(success(12, "type", "Call back", "lines", "Line 1\nLine 2",
                "author", "Group_4", "dateAdded", "2026-10-17"));

        OperationResult<Note> result = adapter.retrieveNote("C1", 4);

        assertEquals(new Note(4, "C1", "Call back", "Line 1\nLine 2", "Group_4", LocalDate.of(2026, 10, 17)),
                result.value());
        assertEquals("4", connection.sent.get(0).getValue("id"));
    }

    @Test
    void addSendsSubjectAndLines() {
        connection.reply(success(13, "customer", "C1", "type", "Call back", "lines", "Hi",
                "author", "Group_4", "id", "8"));

        OperationResult<Note> result = adapter.addNote("C1", "Call back", "Hi");

        assertEquals(8, result.value().id());
        assertEquals("customer.addNote", connection.sent.get(0).getRequestType());
        assertEquals("Call back", connection.sent.get(0).getValue("type"));
        assertEquals("Hi", connection.sent.get(0).getValue("lines"));
    }

    @Test
    void updateSendsTheNoteId() {
        connection.reply(success(14, "customer", "C1", "id", "8", "type", "New",
                "lines", "Hi", "author", "Group_4"));

        OperationResult<Note> result = adapter.updateNote("C1", 8, "New", "Hi");

        assertEquals("New", result.value().type());
        assertEquals("customer.updateNote", connection.sent.get(0).getRequestType());
        assertEquals("8", connection.sent.get(0).getValue("id"));
    }

    @Test
    void listReadsEachNote() {
        connection.reply(success(11, "page", "1", "total", "1", "items", "1",
                "1:id", "4", "1:type", "Call back", "1:author", "Group_4",
                "1:dateAdded", "2026-10-17T09:30:00", "1:numberOfLines", "2"));

        OperationResult<Page<NoteSummary>> result = adapter.listNotes("C1", 1, 50);

        assertEquals(1, result.value().total());
        assertEquals(new NoteSummary(4, "Call back", "Group_4", LocalDate.of(2026, 10, 17), 2),
                result.value().items().get(0));
        assertEquals("customerNote.list", connection.sent.get(0).getRequestType());
    }

    @Test
    void rejectionsArePassedBack() {
        connection.reply(new MessageResponse(new Status(3, "Note not found", 16), new HashMap<>()));

        OperationResult<Note> result = adapter.retrieveNote("C1", 1);

        assertTrue(result.isRejected());
        assertEquals("Note not found", result.errorMessage());
    }

    @Test
    void deleteRejectionIsPassedBack() {
        connection.reply(rejected("Note not found"));
        assertTrue(adapter.deleteNote("C1", 1).isRejected());
    }

    @Test
    void addRejectionIsPassedBack() {
        connection.reply(rejected("Customer not found"));
        assertTrue(adapter.addNote("C1", "Call back", "Hi").isRejected());
    }

    @Test
    void updateRejectionIsPassedBack() {
        connection.reply(rejected("Note not found"));
        assertTrue(adapter.updateNote("C1", 8, "New", "Hi").isRejected());
    }

    @Test
    void connectionFailureIsUnavailable() {
        connection.reply(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(0));

        OperationResult<Page<NoteSummary>> result = adapter.listNotes("C1", 1, 50);

        assertTrue(result.isUnavailable());
    }

    @Test
    void parsesOnlyValidDates() {
        assertNull(NoteAdapter.parseDate(null));
        assertNull(NoteAdapter.parseDate("2026"));
        assertNull(NoteAdapter.parseDate("not-a-date"));
        assertEquals(LocalDate.of(2026, 1, 2), NoteAdapter.parseDate("2026-01-02"));
    }

    @Test
    void parsesLineCountsSafely() {
        assertEquals(0, NoteAdapter.parseCount(null));
        assertEquals(3, NoteAdapter.parseCount(" 3 "));
        assertEquals(0, NoteAdapter.parseCount("x"));
    }

    private static MessageResponse rejected(String message) {
        return new MessageResponse(new Status(3, message, 20), new HashMap<>());
    }

    private static MessageResponse success(long transactionId, String... entries) {
        HashMap<String, String> values = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            values.put(entries[i], entries[i + 1]);
        }
        return new MessageResponse(new Status(transactionId), values);
    }

    /** Returns queued replies in order and remembers what was sent. */
    private static final class FakeConnection implements Connection {
        private final Queue<MessageResponse> replies = new ArrayDeque<>();
        private final List<MessageRequest> sent = new ArrayList<>();

        void reply(MessageResponse response) {
            replies.add(response);
        }

        @Override
        public MessageResponse send(MessageRequest request) {
            sent.add(request);
            return replies.remove();
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public Status Login(String username, String password) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void Logout() {
        }

        @Override
        public void close() {
        }
    }
}