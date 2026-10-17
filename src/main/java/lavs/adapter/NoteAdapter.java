package lavs.adapter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;
import lavs.domain.Note;
import lavs.domain.NoteSummary;
import lavs.domain.Page;
import lavs.gateway.NoteGateway;
import lavs.gateway.OperationResult;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

// AI-assisted: listNotes, parseDate and parseCount from Claude; the other methods adapted by me from Jim's CustomerAdapter
/**
 * Sends customer notes to mainframe, turning replies into domain records.
 */
public class NoteAdapter implements NoteGateway {

    private final mainframe.client.Connection connection;

    public NoteAdapter(mainframe.client.Connection connection) {
        this.connection = connection;
    }

    @Override
    public OperationResult<Boolean> deleteNote(String customerNumber, long id) {
        var message = new mainframe.client.messages.customer.DeleteNote(customerNumber, id);
        try {
            Status status = message.send(connection);
            if (!status.getWasSuccessful()) {
                return OperationResult.failure(
                        status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
            }
            return OperationResult.success(true, status.getTransactionId());
        } catch (IOException e) {
            return OperationResult.failure(
                    StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
        }
    }

    @Override
    public OperationResult<Note> retrieveNote(String customerNumber, long id) {
        var message = new mainframe.client.messages.customerNote.Retrieve(id, customerNumber);
        try {
            Status status = message.send(connection);
            if (!status.getWasSuccessful()) {
                return OperationResult.failure(
                        status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
            }
            return OperationResult.success(
                    new Note(
                            id,
                            customerNumber,
                            message.getType(),
                            message.getLines(),
                            message.getAuthor(),
                            parseDate(message.getDateAdded())),
                    status.getTransactionId());
        } catch (IOException e) {
            return OperationResult.failure(
                    StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
        }
    }

    @Override
    public OperationResult<Note> addNote(String customerNumber, String type, String lines) {
        var message = new mainframe.client.messages.customer.AddNote(customerNumber, lines, type);
        try {
            Status status = message.send(connection);
            if (!status.getWasSuccessful()) {
                return OperationResult.failure(
                        status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
            }
            return OperationResult.success(
                    new Note(
                            message.getId() == null ? 0 : message.getId(),
                            customerNumber,
                            message.getType(),
                            message.getLines(),
                            message.getAuthor(),
                            null),
                    status.getTransactionId());
        } catch (IOException e) {
            return OperationResult.failure(
                    StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
        }
    }

    @Override
    public OperationResult<Note> updateNote(String customerNumber, long id, String type, String lines) {
        var message = new mainframe.client.messages.customer.UpdateNote(customerNumber, id, lines, type);
        try {
            Status status = message.send(connection);
            if (!status.getWasSuccessful()) {
                return OperationResult.failure(
                        status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
            }
            return OperationResult.success(
                    new Note(
                            id,
                            customerNumber,
                            message.getType(),
                            message.getLines(),
                            message.getAuthor(),
                            null),
                    status.getTransactionId());
        } catch (IOException e) {
            return OperationResult.failure(
                    StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
        }
    }

    @Override
    public OperationResult<Page<NoteSummary>> listNotes(String customerNumber, long page, long size) {
        var message = new mainframe.client.messages.customerNote.List(customerNumber, page, size);
        try {
            Status status = message.send(connection);
            if (!status.getWasSuccessful()) {
                return OperationResult.failure(
                        status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
            }
            List<NoteSummary> notes = new ArrayList<>();
            for (var item : message.getItems()) {
                notes.add(new NoteSummary(item.getId(), item.getType(), item.getAuthor(),
                        parseDate(item.getDateAdded()), parseCount(item.getNumberOfLines())));
            }
            return OperationResult.success(
                    new Page<>(notes, message.getPage(), size, message.getTotal()),
                    status.getTransactionId());
        } catch (IOException e) {
            return OperationResult.failure(
                    StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
        }
    }

    // The mainframe may send a date or a date-time; only the date part is kept.
    static LocalDate parseDate(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static long parseCount(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
