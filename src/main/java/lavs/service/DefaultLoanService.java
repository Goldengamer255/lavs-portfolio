package lavs.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lavs.domain.CoBorrower;
import lavs.domain.Loan;
import lavs.domain.NewLoan;
import lavs.domain.Page;
import lavs.gateway.LoanGateway;
import lavs.gateway.OperationResult;
import lavs.validation.LoanField;
import lavs.validation.LoanRules;
import lavs.validation.ValidationService;

/**
 * Default loan business service. It validates and parses raw form input before
 * delegating persistence and applies status and co-borrower workflow rules.
 */
public final class DefaultLoanService implements LoanService {

    private static final ValidationReport VALID = new ValidationReport(List.of());

    private final LoanGateway gateway;
    private final ValidationService validation;
    private final LoanRules rules;

    /**
     * @param gateway loan persistence boundary
     * @param validation form validation supplied by the validation layer
     * @param rules loan status-transition rules
     */
    public DefaultLoanService(
            LoanGateway gateway, ValidationService validation, LoanRules rules) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    @Override
    public OperationResult<Loan> get(String loanNumber) {
        return gateway.retrieve(loanNumber);
    }

    @Override
    public OperationResult<SaveResult<Loan>> create(
            String customerNumber, LoanInput input) {
        ValidationReport report = validateLoan(input);
        if (hasErrors(report)) {
            return invalid(report);
        }

        NewLoan loan = new NewLoan(
                customerNumber,
                Double.parseDouble(input.principal()),
                Double.parseDouble(input.paymentAmount()),
                Double.parseDouble(input.rateAmount()),
                input.rateType(), input.rateFrequency(), input.paymentFrequency(),
                input.startDate(), input.renewalDate());
        return saved(gateway.create(loan));
    }

    @Override
    public OperationResult<SaveResult<Loan>> update(String loanNumber, LoanInput input) {
        ValidationReport report = validateLoan(input);
        if (hasErrors(report)) {
            return invalid(report);
        }

        OperationResult<Loan> existing = gateway.retrieve(loanNumber);
        if (!existing.isSuccess()) {
            return copyFailure(existing);
        }
        Loan current = existing.value();
        Loan updated = new Loan(
                loanNumber, current.customerNumber(),
                Double.parseDouble(input.principal()),
                Double.parseDouble(input.paymentAmount()),
                Double.parseDouble(input.rateAmount()),
                input.rateType(), input.rateFrequency(), input.paymentFrequency(),
                input.startDate(), input.renewalDate(), current.status());
        return saved(gateway.update(updated));
    }

    @Override
    public ValidationReport validateLoan(LoanInput input) {
        ValidationReport report = validation.validateLoan(input);
        List<ValidationIssue> issues = new ArrayList<>(report.errors());
        addNumberIssue(issues, LoanField.PRINCIPAL.key(), input.principal(), "Principal");
        addNumberIssue(issues, LoanField.PAYMENT_AMOUNT.key(), input.paymentAmount(), "Payment amount");
        addNumberIssue(issues, LoanField.RATE_AMOUNT.key(), input.rateAmount(), "Rate amount");
        return issues.equals(report.errors()) ? report : new ValidationReport(issues);
    }

    @Override
    public OperationResult<List<String>> allowedNextStatuses(String loanNumber) {
        OperationResult<Loan> loan = gateway.retrieve(loanNumber);
        if (!loan.isSuccess()) {
            return copyFailure(loan);
        }
        return OperationResult.success(
                List.copyOf(rules.allowedNextStatuses(loan.value().status())),
                loan.transactionId());
    }

    @Override
    public OperationResult<String> changeStatus(String loanNumber, String status) {
        return gateway.changeStatus(loanNumber, status);
    }

    @Override
    public OperationResult<Page<CoBorrower>> listCoBorrowers(
            String loanNumber, long page, long size) {
        return gateway.listCoborrowers(loanNumber, page, size);
    }

    @Override
    public OperationResult<CoBorrower> addCoBorrower(
            String loanNumber, String customerNumber, String type) {
        OperationResult<Long> added = gateway.addCoborrower(loanNumber, customerNumber, type);
        if (!added.isSuccess()) {
            return copyFailure(added);
        }
        return gateway.retrieveCoborrower(loanNumber, added.value());
    }

    @Override
    public OperationResult<Boolean> removeCoBorrower(String loanNumber, long id) {
        return gateway.deleteCoborrower(loanNumber, id);
    }

    @Override
    public OperationResult<CoBorrower> changeCoBorrowerType(
            String loanNumber, long id, String type) {
        OperationResult<CoBorrower> existing = gateway.retrieveCoborrower(loanNumber, id);
        if (!existing.isSuccess()) {
            return copyFailure(existing);
        }

        OperationResult<Boolean> removed = gateway.deleteCoborrower(loanNumber, id);
        if (!removed.isSuccess()) {
            return copyFailure(removed);
        }

        OperationResult<Long> added = gateway.addCoborrower(
                loanNumber, existing.value().customerNumber(), type);
        if (!added.isSuccess()) {
            return partialFailure(
                    added, "The old co-borrower link was removed, but the new type was not added. ");
        }

        OperationResult<CoBorrower> replacement =
                gateway.retrieveCoborrower(loanNumber, added.value());
        if (!replacement.isSuccess()) {
            return partialFailure(
                    replacement, "The type was changed, but the updated link could not be loaded. ");
        }
        return replacement;
    }

    private void addNumberIssue(
            List<ValidationIssue> issues, String fieldKey, String value, String label) {
        boolean alreadyReported = issues.stream()
                .anyMatch(issue -> fieldKey.equals(issue.fieldKey()));
        if (!alreadyReported && !isDouble(value)) {
            issues.add(new ValidationIssue(fieldKey, label + " must be a number."));
        }
    }

    private boolean isDouble(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean hasErrors(ValidationReport report) {
        return !report.errors().isEmpty();
    }

    private <T> OperationResult<SaveResult<T>> invalid(ValidationReport report) {
        return OperationResult.success(new SaveResult<>(null, report), 0);
    }

    private <T> OperationResult<SaveResult<T>> saved(OperationResult<T> result) {
        if (!result.isSuccess()) {
            return copyFailure(result);
        }
        return OperationResult.success(new SaveResult<>(result.value(), VALID),
                result.transactionId());
    }

    private <T> OperationResult<T> partialFailure(
            OperationResult<?> result, String prefix) {
        String detail = result.errorMessage() == null ? "" : result.errorMessage();
        return OperationResult.failure(
                result.errorCode(), prefix + detail, result.transactionId());
    }

    private <T> OperationResult<T> copyFailure(OperationResult<?> result) {
        return OperationResult.failure(
                result.errorCode(), result.errorMessage(), result.transactionId());
    }
}
