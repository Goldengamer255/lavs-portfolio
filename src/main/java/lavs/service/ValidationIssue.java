package lavs.service;

/** A field key matching an input record component, or "_global" for a form error. */
public record ValidationIssue(String fieldKey, String message) {}
