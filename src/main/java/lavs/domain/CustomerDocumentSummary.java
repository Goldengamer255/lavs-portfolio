package lavs.domain;

/**
 * Document list entry containing its identifier, file name, and document category.
 * Retrieve the full {@link CustomerDocument} for file type and size before editing.
 */
public record CustomerDocumentSummary(Long id, String fileName, String type) {}
