package lavs.gateway;

import lavs.domain.Note;
import lavs.domain.NoteSummary;
import lavs.domain.Page;

/**
 * Provides customer note persistence independently of customer identity operations.
 * Note text uses newline-separated lines; the mainframe supplies author and date
 * information in returned records.
 */
public interface NoteGateway {
    /** Lists note summaries without their text, using a one-based page and positive size. */
    OperationResult<Page<NoteSummary>> listNotes(String customerNumber, long page, long size);

    /** Retrieves the complete note, including its text, for display or editing. */
    OperationResult<Note> retrieveNote(String customerNumber, long id);

    OperationResult<Note> addNote(String customerNumber, String type, String lines);

    OperationResult<Note> updateNote(String customerNumber, long id, String type, String lines);

    /** Deletes the customer note and acknowledges success with true. */
    OperationResult<Boolean> deleteNote(String customerNumber, long id);
}
