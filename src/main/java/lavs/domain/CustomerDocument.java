package lavs.domain;

/**
 * Saved document metadata associated with a customer, including its identifier.
 * The size is measured in bytes; this record contains no file contents.
 */
public record CustomerDocument(
    Long id, String customerNumber, String fileName, String fileType, long size, String type) {}
