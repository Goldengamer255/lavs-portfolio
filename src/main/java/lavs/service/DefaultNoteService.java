package lavs.service;

import java.util.ArrayList;
import java.util.List;
import lavs.domain.Note;
import lavs.domain.NoteSummary;
import lavs.domain.Page;
import lavs.gateway.NoteGateway;
import lavs.gateway.OperationResult;
import lavs.validation.NoteField;

// AI-assisted: subject/lines clean-up and the invalid/saved result helpers from Claude; validation and service methods written by me
/**
 * Validates note input before it reaches the mainframe, and cleans it up so the
 * mainframe gets a trimmed subject and newline-separated lines.
 */
public final class DefaultNoteService implements NoteService {
    static final int MAX_SUBJECT_LENGTH = 50;
    private final NoteGateway gateway;

    public DefaultNoteService(NoteGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public OperationResult<Page<NoteSummary>> listNotes(String customerNumber, long page, long size) {
        return gateway.listNotes(customerNumber, page, size);
    }

    @Override
    public OperationResult<Note> getNote(String customerNumber, long id) {
        return gateway.retrieveNote(customerNumber, id);
    }

    @Override
    public OperationResult<Boolean> deleteNote(String customerNumber, long id) {
        return gateway.deleteNote(customerNumber, id);
    }

    @Override
    public ValidationReport validateNote(NoteInput input) {
        List<ValidationIssue> issues = new ArrayList<>();
        String subject = subject(input);

        if (subject.isEmpty()) {
            issues.add(new ValidationIssue(NoteField.TYPE.key(), "Subject is required."));
        } else if (subject.length() > MAX_SUBJECT_LENGTH) {
            issues.add(new ValidationIssue(NoteField.TYPE.key(),
                    "Subject must be " + MAX_SUBJECT_LENGTH + " characters or fewer."));
        }

        if (lines(input).isEmpty()) {
            issues.add(new ValidationIssue(NoteField.LINES.key(), "Note text is required."));
        }
        return new ValidationReport(issues);
    }

    @Override
    public OperationResult<SaveResult<Note>> addNote(String customerNumber, NoteInput input) {
        ValidationReport report = validateNote(input);
        if (!report.errors().isEmpty()) {
            return invalid(report);
        }
        return saved(gateway.addNote(customerNumber, subject(input), lines(input)));
    }

    @Override
    public OperationResult<SaveResult<Note>> updateNote(String customerNumber, long id, NoteInput input) {
        ValidationReport report = validateNote(input);
        if (!report.errors().isEmpty()) {
            return invalid(report);
        }
        return saved(gateway.updateNote(customerNumber, id, subject(input), lines(input)));
    }

    private static String subject(NoteInput input) {
        return input == null || input.type() == null ? "" : input.type().trim();
    }

    // Windows line endings become plain newlines, and blank lines at the start or
    // end are dropped.
    private static String lines(NoteInput input) {
        if (input == null || input.lines() == null) {
            return "";
        }
        return input.lines().replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    private static OperationResult<SaveResult<Note>> invalid(ValidationReport report) {
        return OperationResult.success(new SaveResult<>(null, report), 0);
    }

    private static OperationResult<SaveResult<Note>> saved(OperationResult<Note> result) {
        if (!result.isSuccess()) {
            return OperationResult.failure(result.errorCode(), result.errorMessage(), result.transactionId());
        }
        return OperationResult.success(new SaveResult<>(result.value(), new ValidationReport(List.of())),
                result.transactionId());
    }
}
