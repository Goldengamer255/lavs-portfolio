package lavs.service;

import lavs.domain.Note;
import lavs.domain.NoteSummary;
import lavs.domain.Page;
import lavs.gateway.OperationResult;

/** Customer notes. Writes validate first; run calls on the shared worker. */
public interface NoteService {
    OperationResult<Page<NoteSummary>> listNotes(String customerNumber, long page, long size);

    /**
     * The list doesn't include the note text, so editing reads the full note first.
     */
    OperationResult<Note> getNote(String customerNumber, long id);

    OperationResult<SaveResult<Note>> addNote(String customerNumber, NoteInput input);

    OperationResult<SaveResult<Note>> updateNote(String customerNumber, long id, NoteInput input);

    OperationResult<Boolean> deleteNote(String customerNumber, long id);

    ValidationReport validateNote(NoteInput input);
}
