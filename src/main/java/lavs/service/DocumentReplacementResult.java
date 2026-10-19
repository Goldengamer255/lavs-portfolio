package lavs.service;

import lavs.domain.CustomerDocument;

/**
 * Outcome of the service's add-then-delete document replacement sequence.
 * A complete result confirms deletion of the original; a partial result retains
 * the confirmed replacement when deletion is unconfirmed. A null replacement
 * means the add was not confirmed, not necessarily that no record was created.
 *
 * @param replacement confirmed new document, or null when its addition is unconfirmed
 * @param oldDeleteConfirmed whether the service received a successful delete response
 * @param errorCode status of the failed step, or zero on complete success
 * @param errorMessage failure details, or null on complete success
 * @param transactionId transaction identifier reported by the final attempted step
 */
public record DocumentReplacementResult(
    CustomerDocument replacement,
    boolean oldDeleteConfirmed,
    int errorCode,
    String errorMessage,
    long transactionId) {
  /** Tests whether deletion of the original document was confirmed. */
  public boolean isSuccess() {
    return oldDeleteConfirmed;
  }

  /** Tests whether a replacement exists but deletion of the original is unconfirmed. */
  public boolean isPartial() {
    return replacement != null && !oldDeleteConfirmed;
  }
}
