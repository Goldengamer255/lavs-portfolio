package lavs.domain;

/**
 * Metadata submitted when adding or replacing a customer document.
 * The size is measured in bytes; the customer and document identifiers are
 * supplied separately, and no file contents are included.
 */
public record DocumentMetadata(String fileName, String fileType, long size, String type) {}
