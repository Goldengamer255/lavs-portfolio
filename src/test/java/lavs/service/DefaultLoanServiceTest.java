package lavs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import lavs.domain.CoBorrower;
import lavs.domain.Loan;
import lavs.domain.LoanSummary;
import lavs.domain.LoanSummaryItem;
import lavs.domain.NewLoan;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;
import lavs.gateway.LoanGateway;
import lavs.gateway.OperationResult;
import lavs.validation.LoanRules;
import lavs.validation.ValidationService;
import org.junit.jupiter.api.Test;

/** Verifies loan business behavior independently of the mainframe transport. */
class DefaultLoanServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate RENEWAL = LocalDate.of(2027, 1, 1);
    private static final ValidationReport VALID = new ValidationReport(List.of());

    @Test
    void createValidatesParsesAndCallsGateway() {
        FakeLoanGateway gateway = new FakeLoanGateway();
        Loan saved = loan("L1", "C1", "New");
        gateway.createResult = OperationResult.success(saved, 14);
        DefaultLoanService service = service(gateway, new TestValidation(), new TestLoanRules());

        OperationResult<SaveResult<Loan>> result = service.create("C1", validInput());

        assertTrue(result.isSuccess());
        assertEquals(saved, result.value().value());
        assertEquals(14, result.transactionId());
        assertEquals(new NewLoan("C1", 1000, 100, 5.5, "Fixed", "Annual",
                "Monthly", START, RENEWAL), gateway.created);
    }

    @Test
    void invalidNumbersReturnAllFieldErrorsWithoutWriting() {
        FakeLoanGateway gateway = new FakeLoanGateway();
        DefaultLoanService service = service(gateway, new TestValidation(), new TestLoanRules());
        LoanInput input = new LoanInput("bad", "", null, "Fixed", "Annual",
                "Monthly", START, RENEWAL);

        OperationResult<SaveResult<Loan>> result = service.create("C1", input);

        assertTrue(result.isSuccess());
        assertNull(result.value().value());
        assertEquals(List.of("principal", "paymentAmount", "rateAmount"),
                result.value().validation().errors().stream()
                        .map(ValidationIssue::fieldKey).toList());
        assertNull(gateway.created);
    }

    @Test
    void updatePreservesCustomerAndStatusFromExistingLoan() {
        FakeLoanGateway gateway = new FakeLoanGateway();
        gateway.retrieveResult = OperationResult.success(loan("L1", "C7", "Pending"), 3);
        gateway.updateResult = OperationResult.success(loan("L1", "C7", "Pending"), 4);
        DefaultLoanService service = service(gateway, new TestValidation(), new TestLoanRules());

        OperationResult<SaveResult<Loan>> result = service.update("L1", validInput());

        assertTrue(result.isSuccess());
        assertEquals("C7", gateway.updated.customerNumber());
        assertEquals("Pending", gateway.updated.status());
        assertEquals(1000, gateway.updated.principal());
    }

    @Test
    void validationReportPreventsUpdateRetrieveAndWrite() {
        FakeLoanGateway gateway = new FakeLoanGateway();
        TestValidation validation = new TestValidation();
        validation.loanReport = new ValidationReport(
                List.of(new ValidationIssue("rateType", "Required")));
        DefaultLoanService service = service(gateway, validation, new TestLoanRules());

        OperationResult<SaveResult<Loan>> result = service.update("L1", validInput());

        assertNull(result.value().value());
        assertEquals("rateType", result.value().validation().errors().get(0).fieldKey());
        assertEquals(0, gateway.retrieveCalls);
        assertNull(gateway.updated);
    }

    @Test
    void allowedStatusesUseCurrentGatewayStatus() {
        FakeLoanGateway gateway = new FakeLoanGateway();
        gateway.retrieveResult = OperationResult.success(loan("L1", "C1", "New"), 22);
        TestLoanRules rules = new TestLoanRules();
        rules.allowed = List.of("Pending", "Cancelled");
        DefaultLoanService service = service(gateway, new TestValidation(), rules);

        OperationResult<List<String>> result = service.allowedNextStatuses("L1");

        assertEquals("New", rules.receivedStatus);
        assertEquals(List.of("Pending", "Cancelled"), result.value());
        assertEquals(22, result.transactionId());
    }

    @Test
    void addCoBorrowerLoadsAndReturnsCreatedLink() {
        FakeLoanGateway gateway = new FakeLoanGateway();
        CoBorrower link = new CoBorrower(9, "L1", "C2", "Alex", "Guarantor");
        gateway.addResult = OperationResult.success(9L, 30);
        gateway.retrieveCoBorrowerResult = OperationResult.success(link, 31);
        DefaultLoanService service = service(gateway, new TestValidation(), new TestLoanRules());

        OperationResult<CoBorrower> result =
                service.addCoBorrower("L1", "C2", "Guarantor");

        assertEquals(link, result.value());
        assertEquals(9, gateway.retrievedCoBorrowerId);
    }

    @Test
    void changeCoBorrowerTypeDeletesThenAddsAndReloads() {
        FakeLoanGateway gateway = new FakeLoanGateway();
        gateway.retrieveCoBorrowerResult = OperationResult.success(
                new CoBorrower(5, "L1", "C2", "Alex", "Borrower"), 1);
        gateway.deleteResult = OperationResult.success(true, 2);
        gateway.addResult = OperationResult.success(8L, 3);
        DefaultLoanService service = service(gateway, new TestValidation(), new TestLoanRules());

        OperationResult<CoBorrower> result =
                service.changeCoBorrowerType("L1", 5, "Guarantor");

        assertTrue(result.isSuccess());
        assertEquals("C2", gateway.addedCustomerNumber);
        assertEquals("Guarantor", gateway.addedType);
        assertEquals(8, gateway.retrievedCoBorrowerId);
    }

    @Test
    void changeTypeReportsPartialFailureWhenReplacementCannotBeAdded() {
        FakeLoanGateway gateway = new FakeLoanGateway();
        gateway.retrieveCoBorrowerResult = OperationResult.success(
                new CoBorrower(5, "L1", "C2", "Alex", "Borrower"), 1);
        gateway.deleteResult = OperationResult.success(true, 2);
        gateway.addResult = OperationResult.failure(10, "Offline", 3);
        DefaultLoanService service = service(gateway, new TestValidation(), new TestLoanRules());

        OperationResult<CoBorrower> result =
                service.changeCoBorrowerType("L1", 5, "Guarantor");

        assertFalse(result.isSuccess());
        assertTrue(result.errorMessage().contains("old co-borrower link was removed"));
        assertTrue(result.errorMessage().contains("Offline"));
    }

    @Test
    void simpleOperationsDelegateWithoutChangingResults() {
        FakeLoanGateway gateway = new FakeLoanGateway();
        Loan expected = loan("L1", "C1", "New");
        gateway.retrieveResult = OperationResult.success(expected, 1);
        gateway.changeStatusResult = OperationResult.success("Pending", 2);
        gateway.listResult = OperationResult.success(
                new Page<>(List.of(), 1, 50, 0), 3);
        gateway.deleteResult = OperationResult.success(true, 4);
        DefaultLoanService service = service(gateway, new TestValidation(), new TestLoanRules());

        assertEquals(expected, service.get("L1").value());
        assertEquals("Pending", service.changeStatus("L1", "Pending").value());
        assertEquals(0, service.listCoBorrowers("L1", 1, 50).value().total());
        assertTrue(service.removeCoBorrower("L1", 7).value());
    }

    private DefaultLoanService service(
            FakeLoanGateway gateway, TestValidation validation, TestLoanRules rules) {
        return new DefaultLoanService(gateway, validation, rules);
    }

    private LoanInput validInput() {
        return new LoanInput("1000", "100", "5.5", "Fixed", "Annual",
                "Monthly", START, RENEWAL);
    }

    private static Loan loan(String loanNumber, String customerNumber, String status) {
        return new Loan(loanNumber, customerNumber, 1000, 100, 5.5, "Fixed",
                "Annual", "Monthly", START, RENEWAL, status);
    }

    private static final class TestValidation extends ValidationService {
        private ValidationReport loanReport = VALID;

        @Override
        public ValidationReport validateLoan(LoanInput input) {
            return loanReport;
        }
    }

    private static final class TestLoanRules extends LoanRules {
        private List<String> allowed = List.of();
        private String receivedStatus;

        @Override
        public List<String> allowedNextStatuses(String currentStatus) {
            receivedStatus = currentStatus;
            return allowed;
        }
    }

    private static final class FakeLoanGateway implements LoanGateway {
        private OperationResult<Loan> createResult;
        private OperationResult<Loan> retrieveResult;
        private OperationResult<String> changeStatusResult;
        private OperationResult<Loan> updateResult;
        private OperationResult<Page<CoBorrower>> listResult;
        private OperationResult<Long> addResult;
        private OperationResult<Boolean> deleteResult;
        private OperationResult<CoBorrower> retrieveCoBorrowerResult;
        private NewLoan created;
        private Loan updated;
        private int retrieveCalls;
        private long retrievedCoBorrowerId;
        private String addedCustomerNumber;
        private String addedType;

        @Override
        public OperationResult<Loan> create(NewLoan input) {
            created = input;
            return createResult;
        }

        @Override
        public OperationResult<Loan> retrieve(String loanNumber) {
            retrieveCalls++;
            return retrieveResult;
        }

        @Override
        public OperationResult<String> changeStatus(String loanNumber, String newStatus) {
            return changeStatusResult;
        }

        @Override
        public OperationResult<LoanSummary> getSummary(String loanNumber) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OperationResult<Loan> update(Loan loan) {
            updated = loan;
            return updateResult;
        }

        @Override
        public OperationResult<Page<RepaymentScheduleItem>> getRepaymentSchedule(
                String loanNumber, long page, long size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OperationResult<Page<LoanSummaryItem>> listLoans(
                String customerNumber, long page, long size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OperationResult<Long> addCoborrower(
                String loanNumber, String coborrowerNumber, String type) {
            addedCustomerNumber = coborrowerNumber;
            addedType = type;
            return addResult;
        }

        @Override
        public OperationResult<Boolean> deleteCoborrower(String loanNumber, long id) {
            return deleteResult;
        }

        @Override
        public OperationResult<Page<CoBorrower>> listCoborrowers(
                String loanNumber, long page, long size) {
            return listResult;
        }

        @Override
        public OperationResult<CoBorrower> retrieveCoborrower(String loanNumber, long id) {
            retrievedCoBorrowerId = id;
            return retrieveCoBorrowerResult;
        }
    }
}
