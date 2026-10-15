package lavs.validation;

import lavs.service.CustomerInput;
import lavs.service.ValidationReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationServiceTest {

    private final ValidationService service = new ValidationService();

    @Test
    void validCustomerHasNoIssues() {
        // 1. Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "Lovelace", LocalDate.of(1990, 1, 1), "NZ", "Ms");

        // 2. Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // 3. Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void nullFirstName() {
        // 1. Arrange: build the input
        CustomerInput input = new CustomerInput(null, "Smith", LocalDate.of(1990, 1, 1), "NZ", "Ms");

        // 2. Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.FIRST_NAME.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullFamilyNameIsRejected() {
        // 1. Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", null, LocalDate.of(1990, 1, 1), "NZ", "Ms");

        // 2. Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.FAMILY_NAME.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankFamilyNameIsRejected() {
        // 1. Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "  ", LocalDate.of(1990, 1, 1), "NZ", "Ms");

        // 2. Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.FAMILY_NAME.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void dateOfBirthIsFutureRejected() {
        // 1. Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "davis", LocalDate.now().plusDays(1), "NZ", "Ms");

        // 2. Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.DATE_OF_BIRTH.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullTitleIsRejected() {
        // 1. Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "davis", LocalDate.of(1990, 1, 1), "NZ", null);

        // 2. Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.TITLE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullNationalityIsRejected() {
        // 1. Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "davis", LocalDate.of(1990, 1, 1), null, "Ms");

        // 2. Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.NATIONALITY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void bothInputsIsRejected() {
        // 1. Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "  ", LocalDate.now().plusDays(1), "NZ", "Ms");

        // 2. Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // 3. Assert: check the result
        assertEquals(2, report.errors().size());
        assertEquals(CustomerField.FAMILY_NAME.key(), report.errors().get(0).fieldKey());
        assertEquals(CustomerField.DATE_OF_BIRTH.key(), report.errors().get(1).fieldKey());
    }
}