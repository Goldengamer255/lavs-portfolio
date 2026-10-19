package lavs.gateway;

/**
 * Carries a gateway response value together with status and transaction details.
 * Success is determined by the error code, not by whether the value is present.
 * Factory methods establish the usual success/failure field combinations;
 * the record constructor does not enforce them.
 *
 * @param <T> response value type
 * @param value response payload; null for results created by {@link #failure}
 * @param errorCode status code: zero for success, 10 for unavailable, 3 for rejected
 * @param errorMessage failure description, or null for results created by {@link #success}
 * @param transactionId server transaction identifier, or zero when none is supplied
 */
public record OperationResult<T>(
    T value,
    int errorCode,
    String errorMessage,
    long transactionId
) {
    /** Tests for the success status code without inspecting the payload. */
    public boolean isSuccess() {
        return errorCode == 0;
    }

    /** Creates a successful result with no error message. */
    public static <T> OperationResult<T> success(T value, long transactionId) {
        return new OperationResult<>(value, 0, null, transactionId);
    }

    /** Creates a result with a null payload; callers supply a nonzero failure code. */
    public static <T> OperationResult<T> failure(int errorCode, String errorMessage, long transactionId) {
        return new OperationResult<>(null, errorCode, errorMessage, transactionId);
    }

    /** Tests for the shared unavailable status code (10). */
    public boolean isUnavailable() {
        return errorCode == 10;
    }   

    /** Tests for the shared rejection status code (3). */
    public boolean isRejected() {
        return errorCode == 3;
    }
    
}
