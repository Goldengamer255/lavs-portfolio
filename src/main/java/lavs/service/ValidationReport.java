package lavs.service;

import java.util.List;

/** All validation issues; an empty list means valid. No persistence is performed. */
public record ValidationReport(List<ValidationIssue> errors) {
    public ValidationReport {
        errors = List.copyOf(errors);
    }
}
