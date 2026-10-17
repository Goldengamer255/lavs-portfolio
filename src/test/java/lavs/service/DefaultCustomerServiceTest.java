package lavs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import lavs.domain.Address;
import lavs.domain.AddressSummary;
import lavs.domain.CoBorrower;
import lavs.domain.Customer;
import lavs.domain.CustomerSummary;
import lavs.domain.Email;
import lavs.domain.EmailSummary;
import lavs.domain.Employer;
import lavs.domain.EmployerSummary;
import lavs.domain.Loan;
import lavs.domain.LoanSummary;
import lavs.domain.LoanSummaryItem;
import lavs.domain.NewCustomer;
import lavs.domain.NewLoan;
import lavs.domain.Page;
import lavs.domain.Phone;
import lavs.domain.PhoneSummary;
import lavs.domain.RepaymentScheduleItem;
import lavs.domain.Relationship;
import lavs.domain.RelationshipSummary;
import lavs.gateway.CustomerGateway;
import lavs.gateway.LoanGateway;
import lavs.gateway.OperationResult;
import lavs.validation.CustomerRules;
import lavs.validation.DeleteCheck;
import lavs.validation.ValidationService;
import org.junit.jupiter.api.Test;

/** Verifies customer business behavior independently of mainframe transport. */
class DefaultCustomerServiceTest {

    private static final LocalDate DOB = LocalDate.of(1990, 6, 15);
    private static final ValidationReport VALID = new ValidationReport(List.of());
    private static final ValidationReport INVALID = new ValidationReport(
            List.of(new ValidationIssue("field", "Invalid")));

    @Test
    void constructorRejectsInvalidDependenciesAndOrganisation() {
        FakeCustomerGateway customers = new FakeCustomerGateway();
        FakeLoanGateway loans = new FakeLoanGateway();
        TestValidation validation = new TestValidation();
        TestCustomerRules rules = new TestCustomerRules();

        assertThrows(NullPointerException.class,
                () -> new DefaultCustomerService(null, loans, validation, rules, 0));
        assertThrows(NullPointerException.class,
                () -> new DefaultCustomerService(customers, null, validation, rules, 0));
        assertThrows(NullPointerException.class,
                () -> new DefaultCustomerService(customers, loans, null, rules, 0));
        assertThrows(NullPointerException.class,
                () -> new DefaultCustomerService(customers, loans, validation, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultCustomerService(customers, loans, validation, rules, -1));
    }

    @Test
    void createValidatesAndAssignsConfiguredOrganisation() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        Customer saved = customer("C1", "Active");
        gateway.createResult = OperationResult.success(saved, 12);
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        OperationResult<SaveResult<Customer>> result = service.create(customerInput());

        assertTrue(result.isSuccess());
        assertEquals(saved, result.value().value());
        assertEquals(12, result.transactionId());
        assertEquals(new NewCustomer("Ana", "Singh", DOB, "NZ", 44, "Ms"),
                gateway.createdCustomer);
    }

    @Test
    void createUsesZeroToRequestTheLoggedInUsersOrganisation() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        gateway.createResult = OperationResult.success(customer("C1", "Active"), 12);
        DefaultCustomerService service = new DefaultCustomerService(
                gateway, new FakeLoanGateway(), new TestValidation(),
                new TestCustomerRules(), 0);

        OperationResult<SaveResult<Customer>> result = service.create(customerInput());

        assertTrue(result.isSuccess());
        assertEquals(0, gateway.createdCustomer.organisation());
    }

    @Test
    void invalidCustomerDoesNotCallGateway() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        TestValidation validation = new TestValidation();
        validation.customerReport = new ValidationReport(
                List.of(new ValidationIssue("familyName", "Required")));
        DefaultCustomerService service = service(
                gateway, new FakeLoanGateway(), validation, new TestCustomerRules());

        OperationResult<SaveResult<Customer>> result = service.create(customerInput());

        assertNull(result.value().value());
        assertEquals("familyName", result.value().validation().errors().get(0).fieldKey());
        assertNull(gateway.createdCustomer);
    }

    @Test
    void updatePreservesExistingStatus() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        gateway.retrieveCustomerResult = OperationResult.success(customer("C1", "Pending"), 1);
        gateway.updateCustomerResult = OperationResult.success(customer("C1", "Pending"), 2);
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        OperationResult<SaveResult<Customer>> result =
                service.update("C1", customerInput());

        assertTrue(result.isSuccess());
        assertEquals("Pending", gateway.updatedCustomer.status());
        assertEquals("Ana", gateway.updatedCustomer.firstName());
    }

    @Test
    void invalidUpdateAndRetrieveFailureDoNotWrite() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        TestValidation validation = new TestValidation();
        validation.customerReport = INVALID;
        DefaultCustomerService service = service(
                gateway, new FakeLoanGateway(), validation, new TestCustomerRules());

        assertTrue(service.update("C1", customerInput()).isSuccess());
        assertNull(gateway.updatedCustomer);

        validation.customerReport = VALID;
        gateway.retrieveCustomerResult = OperationResult.failure(10, "Offline", 31);
        OperationResult<SaveResult<Customer>> failed = service.update("C1", customerInput());
        assertTrue(failed.isUnavailable());
        assertEquals(31, failed.transactionId());
    }

    @Test
    void employerValidationAddsParsingErrorAlongsideOtherErrors() {
        TestValidation validation = new TestValidation();
        validation.employerReport = new ValidationReport(
                List.of(new ValidationIssue("name", "Required")));
        DefaultCustomerService service = service(new FakeCustomerGateway(),
                new FakeLoanGateway(), validation, new TestCustomerRules());

        ValidationReport report = service.validateEmployer(
                new EmployerInput("", "", "not-money", "Current"));

        assertEquals(List.of("name", "salary"), report.errors().stream()
                .map(ValidationIssue::fieldKey).toList());
    }

    @Test
    void listAddressesExpandsGatewaySummariesToEditableRecords() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        Address address = address(7L);
        gateway.addressSummaries = OperationResult.success(
                new Page<>(List.of(new AddressSummary(7L, "Auckland", "NZ", "Home", true, true)),
                        1, 50, 1), 20);
        gateway.addressResult = OperationResult.success(address, 21);
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        OperationResult<Page<Address>> result = service.listAddresses("C1", 1, 50);

        assertEquals(List.of(address), result.value().items());
        assertEquals(1, result.value().total());
        assertEquals(21, result.transactionId());
        assertEquals(7, gateway.retrievedAddressId);
    }

    @Test
    void relationshipWritesMapRawInputsToDomainRecords() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        gateway.addAddressResult = OperationResult.success(address(1L), 1);
        gateway.addPhoneResult = OperationResult.success(phone(2L), 2);
        gateway.addEmailResult = OperationResult.success(email(3L), 3);
        gateway.addEmployerResult = OperationResult.success(employer(4L), 4);
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        service.addAddress("C1", addressInput());
        service.addPhone("C1", phoneInput());
        service.addEmail("C1", emailInput());
        service.addEmployer("C1", employerInput());

        assertEquals("Queen Street", gateway.addedAddress.line1());
        assertEquals("021123456", gateway.addedPhone.number());
        assertEquals("ana@example.com", gateway.addedEmail.address());
        assertEquals(85000, gateway.addedEmployer.salary());
    }

    @Test
    void allContactWritesSupportUpdatesAndValidationFailures() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        gateway.addressResult = OperationResult.success(address(7L), 1);
        gateway.phoneResult = OperationResult.success(phone(8L), 2);
        gateway.emailResult = OperationResult.success(email(9L), 3);
        gateway.employerResult = OperationResult.success(employer(10L), 4);
        TestValidation validation = new TestValidation();
        DefaultCustomerService service = service(
                gateway, new FakeLoanGateway(), validation, new TestCustomerRules());

        assertEquals(address(7L), service.updateAddress("C1", 7, addressInput()).value().value());
        assertEquals(phone(8L), service.updatePhone("C1", 8, phoneInput()).value().value());
        assertEquals(email(9L), service.updateEmail("C1", 9, emailInput()).value().value());
        assertEquals(employer(10L), service.updateEmployer("C1", 10, employerInput()).value().value());
        assertEquals(Long.valueOf(7), gateway.updatedAddress.id());
        assertEquals(Long.valueOf(8), gateway.updatedPhone.id());
        assertEquals(Long.valueOf(9), gateway.updatedEmail.id());
        assertEquals(Long.valueOf(10), gateway.updatedEmployer.id());

        validation.addressReport = INVALID;
        validation.phoneReport = INVALID;
        validation.emailReport = INVALID;
        validation.employerReport = INVALID;
        assertNull(service.addAddress("C1", addressInput()).value().value());
        assertNull(service.updateAddress("C1", 7, addressInput()).value().value());
        assertNull(service.addPhone("C1", phoneInput()).value().value());
        assertNull(service.updatePhone("C1", 8, phoneInput()).value().value());
        assertNull(service.addEmail("C1", emailInput()).value().value());
        assertNull(service.updateEmail("C1", 9, emailInput()).value().value());
        assertNull(service.addEmployer("C1", employerInput()).value().value());
        assertNull(service.updateEmployer("C1", 10, employerInput()).value().value());
    }

    @Test
    void contactListsExpandSummariesAndDelegateDeletes() {
        FakeCustomerGateway gateway = populatedContactGateway();
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        assertEquals(phone(2L), service.listPhones("C1", 1, 50).value().items().get(0));
        assertEquals(email(3L), service.listEmails("C1", 1, 50).value().items().get(0));
        assertEquals(employer(4L), service.listEmployers("C1", 1, 50).value().items().get(0));
        assertTrue(service.deleteAddress("C1", 1).value());
        assertTrue(service.deletePhone("C1", 2).value());
        assertTrue(service.deleteEmail("C1", 3).value());
        assertTrue(service.deleteEmployer("C1", 4).value());
    }

    @Test
    void contactDeletesReturnLoadFailuresAndRuleRejections() {
        FakeCustomerGateway gateway = populatedContactGateway();
        TestCustomerRules rules = new TestCustomerRules();
        DefaultCustomerService service = service(
                gateway, new FakeLoanGateway(), new TestValidation(), rules);

        gateway.addressSummaries = OperationResult.failure(10, "addresses offline", 40);
        assertTrue(service.deleteAddress("C1", 1).isUnavailable());
        gateway = populatedContactGateway();
        service = service(gateway, new FakeLoanGateway(), new TestValidation(), rules);
        gateway.phoneSummaries = OperationResult.failure(10, "phones offline", 41);
        assertTrue(service.deletePhone("C1", 2).isUnavailable());

        gateway = populatedContactGateway();
        service = service(gateway, new FakeLoanGateway(), new TestValidation(), rules);
        gateway.emailSummaries = OperationResult.failure(10, "emails offline", 42);
        assertTrue(service.deletePhone("C1", 2).isUnavailable());

        gateway = populatedContactGateway();
        service = service(gateway, new FakeLoanGateway(), new TestValidation(), rules);
        gateway.phoneSummaries = OperationResult.failure(10, "phones offline", 43);
        assertTrue(service.deleteEmail("C1", 3).isUnavailable());

        gateway = populatedContactGateway();
        service = service(gateway, new FakeLoanGateway(), new TestValidation(), rules);
        gateway.emailSummaries = OperationResult.failure(10, "emails offline", 44);
        assertTrue(service.deleteEmail("C1", 3).isUnavailable());

        gateway = populatedContactGateway();
        rules.phoneDelete = new DeleteCheck(false, null);
        rules.emailDelete = new DeleteCheck(false, "Keep one contact method");
        service = service(gateway, new FakeLoanGateway(), new TestValidation(), rules);
        assertEquals("The requested deletion is not allowed.",
                service.deletePhone("C1", 2).errorMessage());
        assertEquals("Keep one contact method", service.deleteEmail("C1", 3).errorMessage());
    }

    @Test
    void readinessReturnsTheFirstUnavailableContactResult() {
        FakeCustomerGateway gateway = populatedContactGateway();
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        gateway.addressSummaries = OperationResult.failure(10, "address failure", 50);
        assertEquals("address failure", service.checkLoanReadiness("C1").errorMessage());
        gateway = populatedContactGateway();
        gateway.phoneSummaries = OperationResult.failure(10, "phone failure", 51);
        service = service(gateway, new FakeLoanGateway(), new TestValidation(), new TestCustomerRules());
        assertEquals("phone failure", service.checkLoanReadiness("C1").errorMessage());
        gateway = populatedContactGateway();
        gateway.emailSummaries = OperationResult.failure(10, "email failure", 52);
        service = service(gateway, new FakeLoanGateway(), new TestValidation(), new TestCustomerRules());
        assertEquals("email failure", service.checkLoanReadiness("C1").errorMessage());
    }

    @Test
    void pageExpansionHandlesSummaryAndDetailFailuresAndMultiplePages() {
        FakeCustomerGateway gateway = populatedContactGateway();
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        gateway.employerSummaries = OperationResult.failure(3, "No list", 60);
        assertTrue(service.listEmployers("C1", 1, 50).isRejected());
        gateway = populatedContactGateway();
        gateway.employerResult = OperationResult.failure(10, "No detail", 61);
        service = service(gateway, new FakeLoanGateway(), new TestValidation(), new TestCustomerRules());
        assertTrue(service.listEmployers("C1", 1, 50).isUnavailable());

        gateway = populatedContactGateway();
        gateway.addressPages = List.of(
                OperationResult.success(new Page<>(List.of(
                        new AddressSummary(1L, "Auckland", "NZ", "Home", true, true)), 1, 100, 2), 62),
                OperationResult.success(new Page<>(List.of(
                        new AddressSummary(1L, "Auckland", "NZ", "Home", true, true)), 2, 100, 2), 63));
        service = service(gateway, new FakeLoanGateway(), new TestValidation(), new TestCustomerRules());
        OperationResult<ValidationReport> result = service.checkLoanReadiness("C1");
        assertTrue(result.isSuccess());
        assertEquals(2, gateway.addressPageCalls);
    }

    @Test
    void deniedAddressDeletionDoesNotCallDeleteGateway() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        gateway.addressSummaries = OperationResult.success(
                new Page<>(List.of(new AddressSummary(7L, "Auckland", "NZ", "Home", true, true)),
                        1, 100, 1), 1);
        gateway.addressResult = OperationResult.success(address(7L), 2);
        TestCustomerRules rules = new TestCustomerRules();
        rules.addressDelete = new DeleteCheck(false, "Keep one mailing address");
        DefaultCustomerService service = service(
                gateway, new FakeLoanGateway(), new TestValidation(), rules);

        OperationResult<Boolean> result = service.deleteAddress("C1", 7);

        assertTrue(result.isRejected());
        assertEquals("Keep one mailing address", result.errorMessage());
        assertEquals(0, gateway.deleteAddressCalls);
    }

    @Test
    void readinessUsesCompleteAddressPhoneAndEmailLists() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        gateway.addressSummaries = OperationResult.success(
                new Page<>(List.of(new AddressSummary(1L, "Auckland", "NZ", "Home", true, true)),
                        1, 100, 1), 1);
        gateway.addressResult = OperationResult.success(address(1L), 2);
        gateway.phoneSummaries = OperationResult.success(
                new Page<>(List.of(new PhoneSummary(2L, "+64", "021", "Mobile", true, true)),
                        1, 100, 1), 3);
        gateway.phoneResult = OperationResult.success(phone(2L), 4);
        gateway.emailSummaries = OperationResult.success(
                new Page<>(List.of(new EmailSummary(3L, "ana@example.com", true, "Personal")),
                        1, 100, 1), 5);
        gateway.emailResult = OperationResult.success(email(3L), 6);
        TestCustomerRules rules = new TestCustomerRules();
        rules.readiness = new ValidationReport(
                List.of(new ValidationIssue("_global", "Not ready")));
        DefaultCustomerService service = service(
                gateway, new FakeLoanGateway(), new TestValidation(), rules);

        OperationResult<ValidationReport> result = service.checkLoanReadiness("C1");

        assertEquals("Not ready", result.value().errors().get(0).message());
        assertEquals(1, rules.addresses.size());
        assertEquals(1, rules.phones.size());
        assertEquals(1, rules.emails.size());
    }

    @Test
    void simpleCustomerAndLoanOperationsDelegate() {
        FakeCustomerGateway customers = new FakeCustomerGateway();
        customers.searchResult = OperationResult.success(
                new Page<>(List.of(), 1, 50, 0), 1);
        customers.retrieveCustomerResult = OperationResult.success(customer("C1", "Active"), 2);
        customers.changeStatusResult = OperationResult.success("Inactive", 3);
        FakeLoanGateway loans = new FakeLoanGateway();
        loans.listLoansResult = OperationResult.success(
                new Page<>(List.of(), 1, 50, 0), 4);
        DefaultCustomerService service = service(
                customers, loans, new TestValidation(), new TestCustomerRules());

        assertEquals(0, service.search("Ana", 1, 50).value().total());
        assertEquals("C1", service.get("C1").value().number());
        assertEquals("Inactive", service.changeStatus("C1", "Inactive").value());
        assertEquals(0, service.listLoans("C1", 1, 50).value().total());
    }

    @Test
    void relationshipValidationReturnsAllFormErrors() {
        DefaultCustomerService service = service(new FakeCustomerGateway(),
                new FakeLoanGateway(), new TestValidation(), new TestCustomerRules());

        ValidationReport report = service.validateRelationship(
                "C1", new RelationshipInput("C1", "Other", ""));

        assertEquals(List.of("secondaryCustomerNumber", "otherType"),
                report.errors().stream().map(ValidationIssue::fieldKey).toList());
    }

    @Test
    void relationshipCrudMapsInputsAndExpandsListResults() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        Relationship relationship = relationship(8L);
        gateway.addRelationshipResult = OperationResult.success(relationship, 20);
        gateway.relationshipSummaries = OperationResult.success(
                new Page<>(List.of(new RelationshipSummary(8L, "C2", "Spouse")),
                        1, 50, 1), 21);
        gateway.relationshipResult = OperationResult.success(relationship, 22);
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        OperationResult<SaveResult<Relationship>> added = service.addRelationship(
                "C1", new RelationshipInput(" C2 ", " Spouse ", ""));
        OperationResult<Page<Relationship>> listed =
                service.listRelationships("C1", 1, 50);
        OperationResult<Boolean> deleted = service.deleteRelationship("C1", 8L);

        assertEquals(relationship, added.value().value());
        assertEquals("C2", gateway.addedRelationship.secondaryCustomerNumber());
        assertEquals("Spouse", gateway.addedRelationship.type());
        assertEquals(List.of(relationship), listed.value().items());
        assertEquals(22, listed.transactionId());
        assertTrue(deleted.isSuccess());
        assertEquals(8L, gateway.deletedRelationshipId);
    }

    @Test
    void relationshipValidationAndUpdateCoverNullAndRawInputs() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        gateway.relationshipResult = OperationResult.success(relationship(8L), 22);
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        assertEquals("_global", service.validateRelationship("C1", null)
                .errors().get(0).fieldKey());
        ValidationReport missing = service.validateRelationship(
                "C1", new RelationshipInput(null, null, null));
        assertEquals(List.of("secondaryCustomerNumber", "type"), missing.errors().stream()
                .map(ValidationIssue::fieldKey).toList());
        OperationResult<SaveResult<Relationship>> updated = service.updateRelationship(
                "C1", 8, new RelationshipInput(" C2 ", " Other ", " Family Friend "));
        assertTrue(updated.isSuccess());
        assertEquals(Long.valueOf(8), gateway.updatedRelationship.id());
        assertEquals("Family Friend", gateway.updatedRelationship.otherType());
        assertNull(service.addRelationship("C1", new RelationshipInput("", "", ""))
                .value().value());
        assertNull(service.updateRelationship("C1", 8,
                new RelationshipInput("C1", "Spouse", "")).value().value());
    }

    @Test
    void gatewayWriteFailuresArePreserved() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        gateway.createResult = OperationResult.failure(10, "create", 70);
        gateway.addAddressResult = OperationResult.failure(3, "address", 71);
        gateway.addPhoneResult = OperationResult.failure(10, "phone", 72);
        gateway.addEmailResult = OperationResult.failure(3, "email", 73);
        gateway.addEmployerResult = OperationResult.failure(10, "employer", 74);
        gateway.addRelationshipResult = OperationResult.failure(3, "relationship", 75);
        DefaultCustomerService service = service(gateway, new FakeLoanGateway(),
                new TestValidation(), new TestCustomerRules());

        assertTrue(service.create(customerInput()).isUnavailable());
        assertTrue(service.addAddress("C1", addressInput()).isRejected());
        assertTrue(service.addPhone("C1", phoneInput()).isUnavailable());
        assertTrue(service.addEmail("C1", emailInput()).isRejected());
        assertTrue(service.addEmployer("C1", employerInput()).isUnavailable());
        assertTrue(service.addRelationship("C1",
                new RelationshipInput("C2", "Spouse", "")).isRejected());
    }

    @Test
    void employerValidationAcceptsNumbersAndDoesNotDuplicateSalaryErrors() {
        TestValidation validation = new TestValidation();
        DefaultCustomerService service = service(new FakeCustomerGateway(),
                new FakeLoanGateway(), validation, new TestCustomerRules());
        assertTrue(service.validateEmployer(employerInput()).errors().isEmpty());
        validation.employerReport = new ValidationReport(
                List.of(new ValidationIssue("salary", "Required")));
        assertEquals(1, service.validateEmployer(
                new EmployerInput("A", "B", null, "Current")).errors().size());
    }

    private FakeCustomerGateway populatedContactGateway() {
        FakeCustomerGateway gateway = new FakeCustomerGateway();
        gateway.addressSummaries = OperationResult.success(new Page<>(List.of(
                new AddressSummary(1L, "Auckland", "NZ", "Home", true, true)), 1, 100, 1), 1);
        gateway.addressResult = OperationResult.success(address(1L), 2);
        gateway.phoneSummaries = OperationResult.success(new Page<>(List.of(
                new PhoneSummary(2L, "+64", "021123456", "Mobile", true, true)), 1, 100, 1), 3);
        gateway.phoneResult = OperationResult.success(phone(2L), 4);
        gateway.emailSummaries = OperationResult.success(new Page<>(List.of(
                new EmailSummary(3L, "ana@example.com", true, "Personal")), 1, 100, 1), 5);
        gateway.emailResult = OperationResult.success(email(3L), 6);
        gateway.employerSummaries = OperationResult.success(new Page<>(List.of(
                new EmployerSummary(4L, "Example Ltd", "Current")), 1, 100, 1), 7);
        gateway.employerResult = OperationResult.success(employer(4L), 8);
        return gateway;
    }

    private DefaultCustomerService service(FakeCustomerGateway customers,
            FakeLoanGateway loans, TestValidation validation, TestCustomerRules rules) {
        return new DefaultCustomerService(customers, loans, validation, rules, 44);
    }

    private CustomerInput customerInput() {
        return new CustomerInput("Ana", "Singh", DOB, "NZ", "Ms");
    }

    private AddressInput addressInput() {
        return new AddressInput("Queen Street", "", "CBD", "Auckland", "NZ", "1010",
                "Home", "", true, true);
    }

    private PhoneInput phoneInput() {
        return new PhoneInput("+64", "021123456", "Mobile", "", true, true);
    }

    private EmailInput emailInput() {
        return new EmailInput("ana@example.com", "Personal", "", true);
    }

    private EmployerInput employerInput() {
        return new EmployerInput("Example Ltd", "Engineer", "85000", "Current");
    }

    private static Customer customer(String number, String status) {
        return new Customer(number, "Ana", "Singh", DOB, "NZ", "Ms", status);
    }

    private static Address address(Long id) {
        return new Address(id, "C1", "Queen Street", "", "CBD", "Auckland", "NZ",
                "1010", "Home", "", true, true);
    }

    private static Phone phone(Long id) {
        return new Phone(id, true, "+64", "Ana Singh", "C1", true,
                "021123456", "", "Mobile");
    }

    private static Email email(Long id) {
        return new Email(id, "Ana Singh", "C1", "ana@example.com", true, "", "Personal");
    }

    private static Employer employer(Long id) {
        return new Employer(id, "Ana Singh", "C1", "Example Ltd", "Engineer", 85000, "Current");
    }

    private static Relationship relationship(Long id) {
        return new Relationship(id, "C1", "Ana Singh", "C2", "Sam Singh",
                "Spouse", "");
    }

    private static final class TestValidation extends ValidationService {
        private ValidationReport customerReport = VALID;
        private ValidationReport addressReport = VALID;
        private ValidationReport phoneReport = VALID;
        private ValidationReport emailReport = VALID;
        private ValidationReport employerReport = VALID;

        @Override public ValidationReport validateCustomer(CustomerInput input, boolean creating) {
            return customerReport;
        }
        @Override public ValidationReport validateAddress(AddressInput input) { return addressReport; }
        @Override public ValidationReport validatePhone(PhoneInput input) { return phoneReport; }
        @Override public ValidationReport validateEmail(EmailInput input) { return emailReport; }
        @Override public ValidationReport validateEmployer(EmployerInput input) {
            return employerReport;
        }
    }

    private static final class TestCustomerRules extends CustomerRules {
        private ValidationReport readiness = VALID;
        private DeleteCheck addressDelete = new DeleteCheck(true, null);
        private DeleteCheck phoneDelete = new DeleteCheck(true, null);
        private DeleteCheck emailDelete = new DeleteCheck(true, null);
        private List<Address> addresses;
        private List<Phone> phones;
        private List<Email> emails;

        @Override
        public ValidationReport checkLoanReadiness(
                List<Address> addresses, List<Phone> phones, List<Email> emails) {
            this.addresses = addresses;
            this.phones = phones;
            this.emails = emails;
            return readiness;
        }

        @Override public DeleteCheck canDeleteAddress(List<Address> addresses, long id) {
            return addressDelete;
        }
        @Override public DeleteCheck canDeletePhone(List<Phone> phones, List<Email> emails, long id) {
            return phoneDelete;
        }
        @Override public DeleteCheck canDeleteEmail(List<Phone> phones, List<Email> emails, long id) {
            return emailDelete;
        }
    }

    private static final class FakeCustomerGateway implements CustomerGateway {
        private OperationResult<Customer> createResult;
        private OperationResult<Customer> retrieveCustomerResult;
        private OperationResult<Page<CustomerSummary>> searchResult;
        private OperationResult<Customer> updateCustomerResult;
        private OperationResult<String> changeStatusResult;
        private OperationResult<Page<AddressSummary>> addressSummaries;
        private OperationResult<Address> addressResult;
        private OperationResult<Address> addAddressResult;
        private OperationResult<Page<PhoneSummary>> phoneSummaries;
        private OperationResult<Phone> phoneResult;
        private OperationResult<Phone> addPhoneResult;
        private OperationResult<Page<EmailSummary>> emailSummaries;
        private OperationResult<Email> emailResult;
        private OperationResult<Email> addEmailResult;
        private OperationResult<Page<EmployerSummary>> employerSummaries;
        private OperationResult<Employer> employerResult;
        private OperationResult<Employer> addEmployerResult;
        private OperationResult<Page<RelationshipSummary>> relationshipSummaries;
        private OperationResult<Relationship> relationshipResult;
        private OperationResult<Relationship> addRelationshipResult;
        private NewCustomer createdCustomer;
        private Customer updatedCustomer;
        private Address addedAddress;
        private Phone addedPhone;
        private Email addedEmail;
        private Employer addedEmployer;
        private Relationship addedRelationship;
        private Address updatedAddress;
        private Phone updatedPhone;
        private Email updatedEmail;
        private Employer updatedEmployer;
        private Relationship updatedRelationship;
        private long deletedRelationshipId;
        private long retrievedAddressId;
        private int deleteAddressCalls;
        private int addressPageCalls;
        private List<OperationResult<Page<AddressSummary>>> addressPages;

        @Override public OperationResult<Customer> create(NewCustomer input) {
            createdCustomer = input; return createResult;
        }
        @Override public OperationResult<Customer> retrieve(String number) {
            return retrieveCustomerResult;
        }
        @Override public OperationResult<Page<CustomerSummary>> search(String name, long page, long size) {
            return searchResult;
        }
        @Override public OperationResult<Customer> update(Customer input) {
            updatedCustomer = input; return updateCustomerResult;
        }
        @Override public OperationResult<String> changeStatus(String number, String status) {
            return changeStatusResult;
        }
        @Override public OperationResult<Address> addAddress(Address input) {
            addedAddress = input; return addAddressResult;
        }
        @Override public OperationResult<Boolean> deleteAddress(String number, long id) {
            deleteAddressCalls++; return OperationResult.success(true, 1);
        }
        @Override public OperationResult<Address> updateAddress(Address input) {
            updatedAddress = input; return addressResult;
        }
        @Override public OperationResult<Page<AddressSummary>> listAddresses(String number, long page, long size) {
            addressPageCalls++;
            if (addressPages != null) return addressPages.get((int) page - 1);
            return addressSummaries;
        }
        @Override public OperationResult<Address> retrieveAddress(String number, long id) {
            retrievedAddressId = id; return addressResult;
        }
        @Override public OperationResult<Phone> addPhone(Phone input) {
            addedPhone = input; return addPhoneResult;
        }
        @Override public OperationResult<Boolean> deletePhone(String number, long id) {
            return OperationResult.success(true, 1);
        }
        @Override public OperationResult<Phone> updatePhone(Phone input) {
            updatedPhone = input; return phoneResult;
        }
        @Override public OperationResult<Page<PhoneSummary>> listPhones(String number, long page, long size) {
            return phoneSummaries;
        }
        @Override public OperationResult<Phone> retrievePhone(String number, long id) { return phoneResult; }
        @Override public OperationResult<Email> addEmail(Email input) {
            addedEmail = input; return addEmailResult;
        }
        @Override public OperationResult<Boolean> deleteEmail(String number, long id) {
            return OperationResult.success(true, 1);
        }
        @Override public OperationResult<Email> updateEmail(Email input) {
            updatedEmail = input; return emailResult;
        }
        @Override public OperationResult<Page<EmailSummary>> listEmails(String number, long page, long size) {
            return emailSummaries;
        }
        @Override public OperationResult<Email> retrieveEmail(String number, long id) { return emailResult; }
        @Override public OperationResult<Employer> addEmployer(Employer input) {
            addedEmployer = input; return addEmployerResult;
        }
        @Override public OperationResult<Boolean> deleteEmployer(String number, long id) {
            return OperationResult.success(true, 1);
        }
        @Override public OperationResult<Employer> updateEmployer(Employer input) {
            updatedEmployer = input; return employerResult;
        }
        @Override public OperationResult<Page<EmployerSummary>> listEmployers(String number, long page, long size) {
            return employerSummaries;
        }
        @Override public OperationResult<Employer> retrieveEmployer(String number, long id) {
            return employerResult;
        }
        @Override public OperationResult<Relationship> addRelationship(Relationship input) {
            addedRelationship = input; return addRelationshipResult;
        }
        @Override public OperationResult<Boolean> deleteRelationship(String number, long id) {
            deletedRelationshipId = id; return OperationResult.success(true, 23);
        }
        @Override public OperationResult<Relationship> updateRelationship(Relationship input) {
            updatedRelationship = input;
            return relationshipResult;
        }
        @Override public OperationResult<Page<RelationshipSummary>> listRelationships(
                String number, long page, long size) {
            return relationshipSummaries;
        }
        @Override public OperationResult<Relationship> retrieveRelationship(
                String number, long id) {
            return relationshipResult;
        }
    }

    private static final class FakeLoanGateway implements LoanGateway {
        private OperationResult<Page<LoanSummaryItem>> listLoansResult;
        @Override public OperationResult<Page<LoanSummaryItem>> listLoans(String number, long page, long size) {
            return listLoansResult;
        }
        @Override public OperationResult<Loan> create(NewLoan input) { throw unsupported(); }
        @Override public OperationResult<Loan> retrieve(String number) { throw unsupported(); }
        @Override public OperationResult<String> changeStatus(String number, String status) { throw unsupported(); }
        @Override public OperationResult<LoanSummary> getSummary(String number) { throw unsupported(); }
        @Override public OperationResult<Loan> update(Loan loan) { throw unsupported(); }
        @Override public OperationResult<Page<RepaymentScheduleItem>> getRepaymentSchedule(String number, long page, long size) { throw unsupported(); }
        @Override public OperationResult<Long> addCoborrower(String loan, String customer, String type) { throw unsupported(); }
        @Override public OperationResult<Boolean> deleteCoborrower(String loan, long id) { throw unsupported(); }
        @Override public OperationResult<Page<CoBorrower>> listCoborrowers(String loan, long page, long size) { throw unsupported(); }
        @Override public OperationResult<CoBorrower> retrieveCoborrower(String loan, long id) { throw unsupported(); }
        private UnsupportedOperationException unsupported() { return new UnsupportedOperationException(); }
    }
}
