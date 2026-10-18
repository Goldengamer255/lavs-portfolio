package lavs.service;

import lavs.domain.CustomerDocument;

/** Keeps the new ID when deleting the old document could not be confirmed. */
public record DocumentReplacementResult(
    CustomerDocument replacement,
    boolean oldDeleteConfirmed,
    int errorCode,
    String errorMessage,
    long transactionId) {
  public boolean isSuccess() {
    return oldDeleteConfirmed;
  }

  public boolean isPartial() {
    return replacement != null && !oldDeleteConfirmed;
  }
}
