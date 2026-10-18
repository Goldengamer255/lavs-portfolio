package lavs.ui;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lavs.domain.Note;
import lavs.domain.NoteSummary;
import lavs.gateway.OperationResult;
import lavs.service.NoteInput;
import lavs.service.NoteService;
import lavs.service.SaveResult;
import lavs.ui.support.EntityListController;
import lavs.ui.support.FormDialog;
import lavs.ui.support.TaskRunner;

// AI-assisted: adapted from my PhonesTab; the edit-loads-full-note step (openExisting/showExisting) from Claude
/**
 * Manages the notes tab: loading, the add/edit popup, saving and deleting.
 */
public final class NotesTab {

    private static final String DIALOG = "/lavs/view/NoteDialog.fxml";
    private static final String LOAD_FAILED = "Couldn't load notes. Check your connection and try again.";
    private static final String SAVE_FAILED = "Not saved yet. The mainframe couldn't be reached. Try Save again.";
    private static final String SAVE_REJECTED = "Not saved. The mainframe said: ";
    private static final String DELETE_FAILED = "Couldn't delete the note. Check your connection and try again.";
    private static final String OPEN_FAILED = "Couldn't open the note. Check your connection and try again.";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EntityListController<NoteSummary> list;
    private final NoteService notes;
    private final TaskRunner tasks;
    private final BooleanProperty saving = new SimpleBooleanProperty(false);

    private String customerNumber;

    /**
     * Sets up the note list's columns and buttons.
     */
    public NotesTab(EntityListController<NoteSummary> list, NoteService notes, TaskRunner tasks) {
        this.list = list;
        this.notes = notes;
        this.tasks = tasks;

        list.addColumn("Subject", NoteSummary::type);
        list.addColumn("Author", NoteSummary::author);
        list.addColumn("Date Added", n -> n.dateAdded() == null ? "" : n.dateAdded().format(DATE_FORMAT));

        list.setEmptyText("No notes yet.");
        list.setOnAdd(() -> openDialog(null));
        list.setOnEdit(this::openExisting);
        list.setOnDelete(this::delete);
    }

    /**
     * Loads the customer's notes.
     */
    public void load(String customerNumber) {
        this.customerNumber = customerNumber;
        list.clearError();
        tasks.run(() -> notes.listAllNotes(customerNumber), list.busyProperty(), this::showNotes,
                error -> list.showError(LOAD_FAILED));
    }

    // The list has no note text, so the full note is read before the popup opens.
    private void openExisting(NoteSummary summary) {
        String number = customerNumber;
        tasks.run(() -> notes.getNote(number, summary.id()), list.busyProperty(),
                this::showExisting, error -> list.showError(OPEN_FAILED));
    }

    private void showExisting(OperationResult<Note> result) {
        if (!result.isSuccess()) {
            list.showError(result.isRejected() ? result.errorMessage() : OPEN_FAILED);
            return;
        }
        openDialog(result.value());
    }

    private void openDialog(Note existing) {
        NoteDialogController form = new NoteDialogController(existing, notes::validateNote);
        String title = existing == null ? "Add Note" : "Edit Note";
        FormDialog.show(list.window(), title, DIALOG, form, saving, close -> save(form, existing, close));
    }

    private void save(NoteDialogController form, Note existing, Runnable close) {
        NoteInput input = form.read();
        String number = customerNumber;
        tasks.run(() -> existing == null
                ? notes.addNote(number, input)
                : notes.updateNote(number, existing.id(), input),
                saving, result -> showSaveResult(result, form, close),
                error -> form.showFormError(SAVE_FAILED));
    }

    private void showNotes(OperationResult<List<NoteSummary>> result) {
        if (!result.isSuccess()) {
            list.showError(LOAD_FAILED);
        } else {
            list.setItems(result.value());
        }
    }

    private void showSaveResult(OperationResult<SaveResult<Note>> result, NoteDialogController form,
            Runnable close) {
        if (!result.isSuccess()) {
            if (result.isRejected()) {
                form.showFormError(SAVE_REJECTED + result.errorMessage());
            } else {
                form.showFormError(SAVE_FAILED);
            }
            return;
        }

        SaveResult<Note> saved = result.value();
        if (saved.value() == null) {
            form.showErrors(saved.validation());
            return;
        }

        close.run();
        load(customerNumber);
    }

    private void delete(NoteSummary note) {
        tasks.run(() -> notes.deleteNote(customerNumber, note.id()),
                list.busyProperty(),
                this::showDeleteResult,
                error -> list.showError(DELETE_FAILED));
    }

    private void showDeleteResult(OperationResult<Boolean> result) {
        if (result.isSuccess()) {
            load(customerNumber);
        } else if (result.isRejected()) {
            list.showError(result.errorMessage());
        } else {
            list.showError(DELETE_FAILED);
        }
    }
}