package lavs.gateway;

import lavs.domain.Note;
import lavs.domain.NoteSummary;
import lavs.domain.Page;

/** Mainframe access for customer notes, kept separate from CustomerGateway. */
public interface NoteGateway {
    OperationResult<Page<NoteSummary>> listNotes(String customerNumber, long page, long size);

    OperationResult<Note> retrieveNote(String customerNumber, long id);

    OperationResult<Note> addNote(String customerNumber, String type, String lines);

    OperationResult<Note> updateNote(String customerNumber, long id, String type, String lines);

    OperationResult<Boolean> deleteNote(String customerNumber, long id);
}
