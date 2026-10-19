package lavs.service;

import java.util.Objects;
import lavs.domain.CustomerDocument;
import lavs.domain.CustomerDocumentSummary;
import lavs.domain.DocumentMetadata;
import lavs.domain.Page;
import lavs.gateway.DocumentGateway;
import lavs.gateway.OperationResult;

/**
 * Delegates customer document metadata operations and coordinates replacement.
 * Calls are synchronous and should run on the shared background worker.
 * Metadata validation is the caller's responsibility; file contents are not transferred.
 */
public final class DocumentService {
  private final DocumentGateway gateway;

  public DocumentService(DocumentGateway gateway) {
    this.gateway = Objects.requireNonNull(gateway);
  }

  public OperationResult<Page<CustomerDocumentSummary>> list(
      String customerNumber, long page, long size) {
    return gateway.listDocuments(customerNumber, page, size);
  }

  public OperationResult<CustomerDocument> get(String customerNumber, long documentId) {
    return gateway.retrieveDocument(customerNumber, documentId);
  }

  public OperationResult<CustomerDocument> add(String customerNumber, DocumentMetadata metadata) {
    return gateway.addDocument(customerNumber, metadata);
  }

  public OperationResult<Boolean> delete(String customerNumber, long documentId) {
    return gateway.deleteDocument(customerNumber, documentId);
  }

  /**
   * Adds replacement metadata before attempting to delete the original record.
   * A confirmed add followed by an unconfirmed delete returns a partial result
   * containing the replacement. No rollback or automatic retry is attempted.
   * Refresh the document list before retrying an unconfirmed operation, since
   * the server may have applied a request whose response was lost.
   */
  public DocumentReplacementResult replace(
      String customerNumber, long oldId, DocumentMetadata metadata) {
    OperationResult<CustomerDocument> added = gateway.addDocument(customerNumber, metadata);
    if (!added.isSuccess()) {
      return new DocumentReplacementResult(
          null, false, added.errorCode(), added.errorMessage(), added.transactionId());
    }

    CustomerDocument replacement = added.value();
    OperationResult<Boolean> deleted = gateway.deleteDocument(customerNumber, oldId);
    if (!deleted.isSuccess()) {
      return new DocumentReplacementResult(
          replacement, false, deleted.errorCode(), deleted.errorMessage(), deleted.transactionId());
    }
    return new DocumentReplacementResult(replacement, true, 0, null, deleted.transactionId());
  }
}
