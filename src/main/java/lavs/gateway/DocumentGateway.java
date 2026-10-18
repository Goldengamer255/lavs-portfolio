package lavs.gateway;

import lavs.domain.CustomerDocument;
import lavs.domain.CustomerDocumentSummary;
import lavs.domain.DocumentMetadata;
import lavs.domain.Page;

/** The four customer-document operations actually provided by the mainframe. */
public interface DocumentGateway {
    OperationResult<CustomerDocument> addDocument(String customerNumber, DocumentMetadata metadata);

    OperationResult<Boolean> deleteDocument(String customerNumber, long documentId);

    OperationResult<Page<CustomerDocumentSummary>> listDocuments(String customerNumber, long page, long size);

    OperationResult<CustomerDocument> retrieveDocument(String customerNumber, long documentId);
}
