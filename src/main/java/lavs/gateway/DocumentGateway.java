package lavs.gateway;

import lavs.domain.CustomerDocument;
import lavs.domain.CustomerDocumentSummary;
import lavs.domain.DocumentMetadata;
import lavs.domain.Page;

/**
 * Provides access to customer document metadata; file contents are not transferred.
 * Replacement is coordinated by the service through separate add and delete
 * operations because the mainframe supplies no document update operation.
 */
public interface DocumentGateway {
    OperationResult<CustomerDocument> addDocument(String customerNumber, DocumentMetadata metadata);

    /** Deletes the identified metadata record and acknowledges success with true. */
    OperationResult<Boolean> deleteDocument(String customerNumber, long documentId);

    /** Lists summaries using a one-based page number and a positive page size. */
    OperationResult<Page<CustomerDocumentSummary>> listDocuments(String customerNumber, long page, long size);

    /** Retrieves complete metadata, including file type and size in bytes. */
    OperationResult<CustomerDocument> retrieveDocument(String customerNumber, long documentId);
}
