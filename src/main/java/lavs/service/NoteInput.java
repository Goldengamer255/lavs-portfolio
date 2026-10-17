package lavs.service;

/** Raw values from the note form: the subject and the note text. */
public record NoteInput(String type, String lines) {
}