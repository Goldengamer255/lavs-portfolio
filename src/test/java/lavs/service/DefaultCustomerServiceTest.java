package lavs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static final class TestValidation extends ValidationService {
        private ValidationReport customerReport = VALID;
        private ValidationReport employerReport = VALID;

        @Override public ValidationReport validateCustomer(CustomerInput input, boolean creating) {
            return customerReport;
        }
        @Override public ValidationReport validateAddress(AddressInput input) { return VALID; }
        @Override public ValidationReport validatePhone(PhoneInput input) { return VALID; }
        @Override public ValidationReport validateEmail(EmailInput input) { return VALID; }
        @Override public ValidationReport validateEmployer(EmployerInput input) {
            return employerReport;
        }
    }

    private static final class TestCustomerRules extends CustomerRules {
        private ValidationReport readiness = VALID;
        private DeleteCheck addressDelete = new DeleteCheck(true, null);
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
        private NewCustomer createdCustomer;
        private Customer updatedCustomer;
        private Address addedAddress;
        private Phone addedPhone;
        private Email addedEmail;
        private Employer addedEmployer;
        private long retrievedAddressId;
        private int deleteAddressCalls;

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
        @Override public OperationResult<Address> updateAddress(Address input) { return addressResult; }
        @Override public OperationResult<Page<AddressSummary>> listAddresses(String number, long page, long size) {
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
        @Override public OperationResult<Phone> updatePhone(Phone input) { return phoneResult; }
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
        @Override public OperationResult<Email> updateEmail(Email input) { return emailResult; }
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
        @Override public OperationResult<Employer> updateEmployer(Employer input) { return employerResult; }
        @Override public OperationResult<Page<EmployerSummary>> listEmployers(String number, long page, long size) {
            return employerSummaries;
        }
        @Override public OperationResult<Employer> retrieveEmployer(String number, long id) {
            return employerResult;
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
