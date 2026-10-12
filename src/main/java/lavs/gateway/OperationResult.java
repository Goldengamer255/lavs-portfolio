package lavs.gateway;

public record OperationResult<T>(
    T value,
    int errorCode,
    String errorMessage,
    long transactionId
) {
    public boolean isSuccess() {
        return errorCode == 0;
    }

    public static <T> OperationResult<T> success(T value, long transactionId) {
        return new OperationResult<>(value, 0, null, transactionId);
    }

    public static <T> OperationResult<T> failure(int errorCode, String errorMessage, long transactionId) {
        return new OperationResult<>(null, errorCode, errorMessage, transactionId);
    }

    public boolean isUnavailable() {
    return errorCode == 10;
    }   

    public boolean isRejected() {
    return errorCode == 3;
    }
    
}
