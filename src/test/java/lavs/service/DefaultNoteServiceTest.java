package lavs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import lavs.domain.Note;
import lavs.domain.NoteSummary;
import lavs.domain.Page;
import lavs.gateway.NoteGateway;
import lavs.gateway.OperationResult;
import lavs.validation.NoteField;
import org.junit.jupiter.api.Test;

/**
 * Checks note validation and that only valid, cleaned-up notes reach the
 * gateway.
 */
class DefaultNoteServiceTest {

    private final FakeNoteGateway gateway = new FakeNoteGateway();
    private final DefaultNoteService service = new DefaultNoteService(gateway);

    @Test
    void validNoteHasNoIssues() {
        ValidationReport report = service.validateNote(new NoteInput("Call back", "Wants a fixed rate"));

        assertTrue(report.errors().isEmpty());
    }

    @Test
    void missingSubjectAndTextAreBothReported() {
        ValidationReport report = service.validateNote(new NoteInput(null, null));

        assertEquals(2, report.errors().size());
        assertEquals(NoteField.TYPE.key(), report.errors().get(0).fieldKey());
        assertEquals(NoteField.LINES.key(), report.errors().get(1).fieldKey());
    }

    @Test
    void blankSubjectAndTextAreRejected() {
        ValidationReport report = service.validateNote(new NoteInput(" ", " \n "));

        assertEquals(2, report.errors().size());
    }

    @Test
    void nullInputIsRejected() {
        ValidationReport report = service.validateNote(null);

        assertEquals(2, report.errors().size());
    }

    @Test
    void subjectOverLimitIsRejected() {
        ValidationReport report = service.validateNote(new NoteInput("x".repeat(51), "t"));

        assertEquals(1, report.errors().size());
        assertEquals(NoteField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void subjectAtLimitIsAccepted() {
        ValidationReport report = service.validateNote(new NoteInput("x".repeat(50), "t"));

        assertTrue(report.errors().isEmpty());
    }

    @Test
    void invalidAddDoesNotCallGateway() {
        OperationResult<SaveResult<Note>> result = service.addNote("C1", new NoteInput("", "t"));

        assertTrue(result.isSuccess());
        assertNull(result.value().value());
        assertEquals(1, result.value().validation().errors().size());
        assertTrue(gateway.calls.isEmpty());
    }

    @Test
    void validAddSendsCleanValues() {
        gateway.noteResult = OperationResult.success(note(), 42);

        OperationResult<SaveResult<Note>> result = service.addNote("C1",
                new NoteInput(" Call back ", "\r\nLine 1\r\nLine 2\r\n"));

        assertEquals(List.of("add C1 Call back|Line 1\nLine 2"), gateway.calls);
        assertEquals(5, result.value().value().id());
        assertEquals(42, result.transactionId());
    }

    @Test
    void rejectedAddIsPassedBack() {
        gateway.noteResult = OperationResult.failure(3, "Customer not found", 7);

        OperationResult<SaveResult<Note>> result = service.addNote("C1", new NoteInput("S", "t"));

        assertTrue(result.isRejected());
        assertEquals("Customer not found", result.errorMessage());
    }

    @Test
    void validUpdateSendsId() {
        gateway.noteResult = OperationResult.success(note(), 1);

        service.updateNote("C1", 5, new NoteInput(" New ", "t "));

        assertEquals(List.of("update C1 5 New|t"), gateway.calls);
    }

    @Test
    void invalidUpdateDoesNotCallGateway() {
        OperationResult<SaveResult<Note>> result = service.updateNote("C1", 5, new NoteInput("S", ""));

        assertNull(result.value().value());
        assertTrue(gateway.calls.isEmpty());
    }

    @Test
    void listGetAndDeleteUseGateway() {
        OperationResult<Page<NoteSummary>> page = OperationResult.success(new Page<>(List.of(), 1, 50, 0), 1);
        OperationResult<Note> one = OperationResult.success(note(), 2);
        OperationResult<Boolean> deleted = OperationResult.success(true, 3);

        gateway.pageResult = page;
        gateway.noteResult = one;
        gateway.deleteResult = deleted;

        assertSame(page, service.listNotes("C1", 1, 50));
        assertSame(one, service.getNote("C1", 5));
        assertSame(deleted, service.deleteNote("C1", 5));
        assertEquals(List.of("list C1", "get C1 5", "delete C1 5"), gateway.calls);
    }

    private static Note note() {
        return new Note(5, "C1", "Subject", "text", "Group_4", null);
    }

    /**
     * Records each call so tests can check exactly what would be sent to the
     * mainframe.
     */
    private static final class FakeNoteGateway implements NoteGateway {
        private final List<String> calls = new ArrayList<>();
        private OperationResult<Page<NoteSummary>> pageResult;
        private OperationResult<Note> noteResult;
        private OperationResult<Boolean> deleteResult;

        @Override
        public OperationResult<Page<NoteSummary>> listNotes(String customerNumber, long page, long size) {
            calls.add("list " + customerNumber);
            return pageResult;
        }

        @Override
        public OperationResult<Note> retrieveNote(String customerNumber, long id) {
            calls.add("get " + customerNumber + " " + id);
            return noteResult;
        }

        @Override
        public OperationResult<Note> addNote(String customerNumber, String type, String lines) {
            calls.add("add " + customerNumber + " " + type + "|" + lines);
            return noteResult;
        }

        @Override
        public OperationResult<Note> updateNote(String customerNumber, long id, String type, String lines) {
            calls.add("update " + customerNumber + " " + id + " " + type + "|" + lines);
            return noteResult;
        }

        @Override
        public OperationResult<Boolean> deleteNote(String customerNumber, long id) {
            calls.add("delete " + customerNumber + " " + id);
            return deleteResult;
        }
    }
}