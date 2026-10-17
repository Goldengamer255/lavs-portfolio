package lavs.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lavs.domain.Address;
import lavs.domain.AddressSummary;
import lavs.domain.Customer;
import lavs.domain.CustomerSummary;
import lavs.domain.Email;
import lavs.domain.EmailSummary;
import lavs.domain.Employer;
import lavs.domain.EmployerSummary;
import lavs.domain.LoanSummaryItem;
import lavs.domain.NewCustomer;
import lavs.domain.Page;
import lavs.domain.Phone;
import lavs.domain.PhoneSummary;
import lavs.domain.Relationship;
import lavs.domain.RelationshipSummary;
import lavs.gateway.CustomerGateway;
import lavs.gateway.LoanGateway;
import lavs.gateway.OperationResult;
import lavs.validation.CustomerRules;
import lavs.validation.DeleteCheck;
import lavs.validation.EmployerField;
import lavs.validation.FieldKey;
import lavs.validation.RelationshipField;
import lavs.validation.ValidationService;

/**
 * Default customer business service. It validates raw form values, applies
 * customer relationship rules, and delegates persistence to gateway contracts.
 */
public final class DefaultCustomerService implements CustomerService {

    private static final int REJECTED = 3;
    private static final long INTERNAL_PAGE_SIZE = 100;
    private static final ValidationReport VALID = new ValidationReport(List.of());

    private final CustomerGateway customers;
    private final LoanGateway loans;
    private final ValidationService validation;
    private final CustomerRules rules;
    private final long organisationId;

    /**
     * @param customers customer persistence boundary
     * @param loans loan persistence boundary used to list a customer's loans
     * @param validation form validation supplied by the validation layer
     * @param rules cross-record customer business rules
     * @param organisationId configured organisation assigned to new customers
     */
    public DefaultCustomerService(CustomerGateway customers, LoanGateway loans,
            ValidationService validation, CustomerRules rules, long organisationId) {
        this.customers = Objects.requireNonNull(customers, "customers");
        this.loans = Objects.requireNonNull(loans, "loans");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.rules = Objects.requireNonNull(rules, "rules");
        if (organisationId <= 0) {
            throw new IllegalArgumentException("organisationId must be positive");
        }
        this.organisationId = organisationId;
    }

    @Override
    public OperationResult<Page<CustomerSummary>> search(String name, long page, long size) {
        return customers.search(name, page, size);
    }

    @Override
    public OperationResult<Customer> get(String customerNumber) {
        return customers.retrieve(customerNumber);
    }

    @Override
    public OperationResult<SaveResult<Customer>> create(CustomerInput input) {
        ValidationReport report = validateCustomer(input, true);
        if (hasErrors(report)) {
            return invalid(report);
        }

        NewCustomer customer = new NewCustomer(
                input.firstName(), input.familyName(), input.dateOfBirth(),
                input.nationality(), organisationId, input.title());
        return saved(customers.create(customer));
    }

    @Override
    public OperationResult<SaveResult<Customer>> update(
            String customerNumber, CustomerInput input) {
        ValidationReport report = validateCustomer(input, false);
        if (hasErrors(report)) {
            return invalid(report);
        }

        OperationResult<Customer> existing = customers.retrieve(customerNumber);
        if (!existing.isSuccess()) {
            return copyFailure(existing);
        }
        Customer current = existing.value();
        Customer updated = new Customer(
                customerNumber, input.firstName(), input.familyName(), input.dateOfBirth(),
                input.nationality(), input.title(), current.status());
        return saved(customers.update(updated));
    }

    @Override
    public OperationResult<String> changeStatus(String customerNumber, String status) {
        return customers.changeStatus(customerNumber, status);
    }

    @Override
    public ValidationReport validateCustomer(CustomerInput input, boolean creating) {
        return validation.validateCustomer(input, creating);
    }

    @Override
    public OperationResult<ValidationReport> checkLoanReadiness(String customerNumber) {
        OperationResult<List<Address>> addresses = loadAll(this::listAddresses, customerNumber);
        if (!addresses.isSuccess()) {
            return copyFailure(addresses);
        }
        OperationResult<List<Phone>> phones = loadAll(this::listPhones, customerNumber);
        if (!phones.isSuccess()) {
            return copyFailure(phones);
        }
        OperationResult<List<Email>> emails = loadAll(this::listEmails, customerNumber);
        if (!emails.isSuccess()) {
            return copyFailure(emails);
        }

        ValidationReport report = rules.checkLoanReadiness(
                addresses.value(), phones.value(), emails.value());
        return OperationResult.success(report, emails.transactionId());
    }

    @Override
    public OperationResult<Page<LoanSummaryItem>> listLoans(
            String customerNumber, long page, long size) {
        return loans.listLoans(customerNumber, page, size);
    }

    @Override
    public OperationResult<Page<Address>> listAddresses(
            String customerNumber, long page, long size) {
        OperationResult<Page<AddressSummary>> summaries =
                customers.listAddresses(customerNumber, page, size);
        return expandPage(summaries,
                summary -> customers.retrieveAddress(customerNumber, summary.id()));
    }

    @Override
    public OperationResult<SaveResult<Address>> addAddress(
            String customerNumber, AddressInput input) {
        ValidationReport report = validateAddress(input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.addAddress(toAddress(customerNumber, null, input)));
    }

    @Override
    public OperationResult<SaveResult<Address>> updateAddress(
            String customerNumber, long id, AddressInput input) {
        ValidationReport report = validateAddress(input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.updateAddress(toAddress(customerNumber, id, input)));
    }

    @Override
    public OperationResult<Boolean> deleteAddress(String customerNumber, long id) {
        OperationResult<List<Address>> addresses = loadAll(this::listAddresses, customerNumber);
        if (!addresses.isSuccess()) {
            return copyFailure(addresses);
        }
        DeleteCheck check = rules.canDeleteAddress(addresses.value(), id);
        if (!check.allowed()) {
            return rejected(check.reason());
        }
        return customers.deleteAddress(customerNumber, id);
    }

    @Override
    public ValidationReport validateAddress(AddressInput input) {
        return validation.validateAddress(input);
    }

    @Override
    public OperationResult<Page<Phone>> listPhones(
            String customerNumber, long page, long size) {
        OperationResult<Page<PhoneSummary>> summaries =
                customers.listPhones(customerNumber, page, size);
        return expandPage(summaries,
                summary -> customers.retrievePhone(customerNumber, summary.id()));
    }

    @Override
    public OperationResult<SaveResult<Phone>> addPhone(
            String customerNumber, PhoneInput input) {
        ValidationReport report = validatePhone(input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.addPhone(toPhone(customerNumber, null, input)));
    }

    @Override
    public OperationResult<SaveResult<Phone>> updatePhone(
            String customerNumber, long id, PhoneInput input) {
        ValidationReport report = validatePhone(input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.updatePhone(toPhone(customerNumber, id, input)));
    }

    @Override
    public OperationResult<Boolean> deletePhone(String customerNumber, long id) {
        OperationResult<List<Phone>> phones = loadAll(this::listPhones, customerNumber);
        if (!phones.isSuccess()) {
            return copyFailure(phones);
        }
        OperationResult<List<Email>> emails = loadAll(this::listEmails, customerNumber);
        if (!emails.isSuccess()) {
            return copyFailure(emails);
        }
        DeleteCheck check = rules.canDeletePhone(phones.value(), emails.value(), id);
        if (!check.allowed()) {
            return rejected(check.reason());
        }
        return customers.deletePhone(customerNumber, id);
    }

    @Override
    public ValidationReport validatePhone(PhoneInput input) {
        return validation.validatePhone(input);
    }

    @Override
    public OperationResult<Page<Email>> listEmails(
            String customerNumber, long page, long size) {
        OperationResult<Page<EmailSummary>> summaries =
                customers.listEmails(customerNumber, page, size);
        return expandPage(summaries,
                summary -> customers.retrieveEmail(customerNumber, summary.id()));
    }

    @Override
    public OperationResult<SaveResult<Email>> addEmail(
            String customerNumber, EmailInput input) {
        ValidationReport report = validateEmail(input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.addEmail(toEmail(customerNumber, null, input)));
    }

    @Override
    public OperationResult<SaveResult<Email>> updateEmail(
            String customerNumber, long id, EmailInput input) {
        ValidationReport report = validateEmail(input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.updateEmail(toEmail(customerNumber, id, input)));
    }

    @Override
    public OperationResult<Boolean> deleteEmail(String customerNumber, long id) {
        OperationResult<List<Phone>> phones = loadAll(this::listPhones, customerNumber);
        if (!phones.isSuccess()) {
            return copyFailure(phones);
        }
        OperationResult<List<Email>> emails = loadAll(this::listEmails, customerNumber);
        if (!emails.isSuccess()) {
            return copyFailure(emails);
        }
        DeleteCheck check = rules.canDeleteEmail(phones.value(), emails.value(), id);
        if (!check.allowed()) {
            return rejected(check.reason());
        }
        return customers.deleteEmail(customerNumber, id);
    }

    @Override
    public ValidationReport validateEmail(EmailInput input) {
        return validation.validateEmail(input);
    }

    @Override
    public OperationResult<Page<Employer>> listEmployers(
            String customerNumber, long page, long size) {
        OperationResult<Page<EmployerSummary>> summaries =
                customers.listEmployers(customerNumber, page, size);
        return expandPage(summaries,
                summary -> customers.retrieveEmployer(customerNumber, summary.id()));
    }

    @Override
    public OperationResult<SaveResult<Employer>> addEmployer(
            String customerNumber, EmployerInput input) {
        ValidationReport report = validateEmployer(input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.addEmployer(toEmployer(customerNumber, null, input)));
    }

    @Override
    public OperationResult<SaveResult<Employer>> updateEmployer(
            String customerNumber, long id, EmployerInput input) {
        ValidationReport report = validateEmployer(input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.updateEmployer(toEmployer(customerNumber, id, input)));
    }

    @Override
    public OperationResult<Boolean> deleteEmployer(String customerNumber, long id) {
        return customers.deleteEmployer(customerNumber, id);
    }

    @Override
    public ValidationReport validateEmployer(EmployerInput input) {
        ValidationReport report = validation.validateEmployer(input);
        List<ValidationIssue> issues = new ArrayList<>(report.errors());
        boolean salaryReported = issues.stream()
                .anyMatch(issue -> EmployerField.SALARY.key().equals(issue.fieldKey()));
        if (!salaryReported && !isDouble(input.salary())) {
            issues.add(new ValidationIssue(
                    EmployerField.SALARY.key(), "Salary must be a number."));
        }
        return issues.equals(report.errors()) ? report : new ValidationReport(issues);
    }

    @Override
    public OperationResult<Page<Relationship>> listRelationships(
            String customerNumber, long page, long size) {
        OperationResult<Page<RelationshipSummary>> summaries =
                customers.listRelationships(customerNumber, page, size);
        return expandPage(summaries,
                summary -> customers.retrieveRelationship(customerNumber, summary.id()));
    }

    @Override
    public OperationResult<SaveResult<Relationship>> addRelationship(
            String customerNumber, RelationshipInput input) {
        ValidationReport report = validateRelationship(customerNumber, input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.addRelationship(toRelationship(customerNumber, null, input)));
    }

    @Override
    public OperationResult<SaveResult<Relationship>> updateRelationship(
            String customerNumber, long id, RelationshipInput input) {
        ValidationReport report = validateRelationship(customerNumber, input);
        if (hasErrors(report)) {
            return invalid(report);
        }
        return saved(customers.updateRelationship(toRelationship(customerNumber, id, input)));
    }

    @Override
    public OperationResult<Boolean> deleteRelationship(String customerNumber, long id) {
        return customers.deleteRelationship(customerNumber, id);
    }

    @Override
    public ValidationReport validateRelationship(
            String customerNumber, RelationshipInput input) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (input == null) {
            return new ValidationReport(List.of(
                    new ValidationIssue(FieldKey.GLOBAL, "Relationship details are required.")));
        }

        String secondaryNumber = trimmed(input.secondaryCustomerNumber());
        String type = trimmed(input.type());
        String otherType = trimmed(input.otherType());
        if (secondaryNumber.isEmpty()) {
            issues.add(new ValidationIssue(
                    RelationshipField.SECONDARY_CUSTOMER_NUMBER.key(),
                    "Related customer number is required."));
        } else if (secondaryNumber.equalsIgnoreCase(trimmed(customerNumber))) {
            issues.add(new ValidationIssue(
                    RelationshipField.SECONDARY_CUSTOMER_NUMBER.key(),
                    "A customer cannot be related to themselves."));
        }
        if (type.isEmpty()) {
            issues.add(new ValidationIssue(
                    RelationshipField.TYPE.key(), "Relationship type is required."));
        }
        if ("Other".equals(type) && otherType.isEmpty()) {
            issues.add(new ValidationIssue(
                    RelationshipField.OTHER_TYPE.key(),
                    "Enter the relationship type when Other is selected."));
        }
        return new ValidationReport(issues);
    }

    private Address toAddress(String customerNumber, Long id, AddressInput input) {
        return new Address(id, customerNumber, input.line1(), input.line2(), input.suburb(),
                input.city(), input.country(), input.postCode(), input.type(), input.otherType(),
                input.isMailing(), input.isPrimary());
    }

    private Phone toPhone(String customerNumber, Long id, PhoneInput input) {
        return new Phone(id, input.canTxt(), input.countryCode(), null, customerNumber,
                input.isPrimary(), input.number(), input.otherType(), input.type());
    }

    private Email toEmail(String customerNumber, Long id, EmailInput input) {
        return new Email(id, null, customerNumber, input.address(), input.isPrimary(),
                input.otherType(), input.type());
    }

    private Employer toEmployer(String customerNumber, Long id, EmployerInput input) {
        return new Employer(id, null, customerNumber, input.name(), input.position(),
                Double.parseDouble(input.salary()), input.type());
    }

    private Relationship toRelationship(
            String customerNumber, Long id, RelationshipInput input) {
        return new Relationship(id, customerNumber, null,
                trimmed(input.secondaryCustomerNumber()), null,
                trimmed(input.type()), trimmed(input.otherType()));
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
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

    private OperationResult<Boolean> rejected(String reason) {
        String message = reason == null || reason.isBlank()
                ? "The requested deletion is not allowed."
                : reason;
        return OperationResult.failure(REJECTED, message, 0);
    }

    private <S, T> OperationResult<Page<T>> expandPage(
            OperationResult<Page<S>> summaries,
            Function<S, OperationResult<T>> retrieve) {
        if (!summaries.isSuccess()) {
            return copyFailure(summaries);
        }

        List<T> details = new ArrayList<>();
        long transactionId = summaries.transactionId();
        for (S summary : summaries.value().items()) {
            OperationResult<T> item = retrieve.apply(summary);
            if (!item.isSuccess()) {
                return copyFailure(item);
            }
            details.add(item.value());
            transactionId = item.transactionId();
        }

        Page<S> source = summaries.value();
        return OperationResult.success(
                new Page<>(details, source.page(), source.size(), source.total()), transactionId);
    }

    private <T> OperationResult<List<T>> loadAll(
            PageLoader<T> loader, String customerNumber) {
        List<T> all = new ArrayList<>();
        long page = 1;
        long transactionId = 0;
        while (true) {
            OperationResult<Page<T>> result =
                    loader.load(customerNumber, page, INTERNAL_PAGE_SIZE);
            if (!result.isSuccess()) {
                return copyFailure(result);
            }
            all.addAll(result.value().items());
            transactionId = result.transactionId();
            if (result.value().items().isEmpty() || all.size() >= result.value().total()) {
                return OperationResult.success(List.copyOf(all), transactionId);
            }
            page++;
        }
    }

    private <T> OperationResult<T> copyFailure(OperationResult<?> result) {
        return OperationResult.failure(
                result.errorCode(), result.errorMessage(), result.transactionId());
    }

    @FunctionalInterface
    private interface PageLoader<T> {
        OperationResult<Page<T>> load(String customerNumber, long page, long size);
    }
}
