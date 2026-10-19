package lavs.service;

import lavs.domain.*;
import lavs.gateway.OperationResult;

/**
 * Customer operations with form validation and cross-record business rules
 * applied by the service layer before the relevant gateway writes.
 * Page numbers are one-based; sizes are positive.
 * Validation methods do not save and return all issues for field-blur feedback.
 * Network operations must run on the shared worker.
 */
// AI-assisted: service contract scaffolding for team review.
public interface CustomerService {
    OperationResult<Page<CustomerSummary>> search(String name, long page, long size);
    OperationResult<Customer> get(String customerNumber);
    /** Uses the organisation configured for the application, not a form field. */
    OperationResult<SaveResult<Customer>> create(CustomerInput input);
    OperationResult<SaveResult<Customer>> update(String customerNumber, CustomerInput input);
    /** Returns the echoed status without re-reading the full record. */
    OperationResult<String> changeStatus(String customerNumber, String status);
    ValidationReport validateCustomer(CustomerInput input, boolean creating);

    /** Checks existing customer prerequisites; loan-level guarantor rules are separate. */
    OperationResult<ValidationReport> checkLoanReadiness(String customerNumber);
    OperationResult<Page<LoanSummaryItem>> listLoans(String customerNumber, long page, long size);

    OperationResult<Page<Address>> listAddresses(String customerNumber, long page, long size);
    OperationResult<SaveResult<Address>> addAddress(String customerNumber, AddressInput input);
    OperationResult<SaveResult<Address>> updateAddress(String customerNumber, long id, AddressInput input);
    OperationResult<Boolean> deleteAddress(String customerNumber, long id);
    ValidationReport validateAddress(AddressInput input);

    OperationResult<Page<Phone>> listPhones(String customerNumber, long page, long size);
    OperationResult<SaveResult<Phone>> addPhone(String customerNumber, PhoneInput input);
    OperationResult<SaveResult<Phone>> updatePhone(String customerNumber, long id, PhoneInput input);
    OperationResult<Boolean> deletePhone(String customerNumber, long id);
    ValidationReport validatePhone(PhoneInput input);

    OperationResult<Page<Email>> listEmails(String customerNumber, long page, long size);
    OperationResult<SaveResult<Email>> addEmail(String customerNumber, EmailInput input);
    OperationResult<SaveResult<Email>> updateEmail(String customerNumber, long id, EmailInput input);
    OperationResult<Boolean> deleteEmail(String customerNumber, long id);
    ValidationReport validateEmail(EmailInput input);

    OperationResult<Page<Employer>> listEmployers(String customerNumber, long page, long size);
    OperationResult<SaveResult<Employer>> addEmployer(String customerNumber, EmployerInput input);
    OperationResult<SaveResult<Employer>> updateEmployer(String customerNumber, long id, EmployerInput input);
    OperationResult<Boolean> deleteEmployer(String customerNumber, long id);
    ValidationReport validateEmployer(EmployerInput input);

    OperationResult<Page<Relationship>> listRelationships(
            String customerNumber, long page, long size);
    OperationResult<SaveResult<Relationship>> addRelationship(
            String customerNumber, RelationshipInput input);
    OperationResult<SaveResult<Relationship>> updateRelationship(
            String customerNumber, long id, RelationshipInput input);
    OperationResult<Boolean> deleteRelationship(String customerNumber, long id);
    ValidationReport validateRelationship(
            String customerNumber, RelationshipInput input);
}
