package lavs.service;

/**
 * Value inside OperationResult for form writes. On validation failure, value is
 * null and validation contains all issues; no gateway write is made. On a saved
 * result, value contains the saved record and validation has no errors.
 * Transport/server failures use the outer OperationResult failure instead.
 */
public record SaveResult<T>(T value, ValidationReport validation) {}
