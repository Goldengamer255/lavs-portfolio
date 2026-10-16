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
        // Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "Lovelace", LocalDate.of(1990, 1, 1), "NZ", "Ms");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void nullFirstName() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput(null, "Smith", LocalDate.of(1990, 1, 1), "NZ", "Ms");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.FIRST_NAME.key(), report.errors().get(0).fieldKey());
    }

    void blankFirstName() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput(" ", "Smith", LocalDate.of(1990, 1, 1), "NZ", "Ms");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.FIRST_NAME.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullFamilyNameIsRejected() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", null, LocalDate.of(1990, 1, 1), "NZ", "Ms");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.FAMILY_NAME.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankFamilyNameIsRejected() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "  ", LocalDate.of(1990, 1, 1), "NZ", "Ms");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.FAMILY_NAME.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void dateOfBirthIsFutureRejected() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "davis", LocalDate.now().plusDays(1), "NZ", "Ms");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.DATE_OF_BIRTH.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullTitleIsRejected() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "davis", LocalDate.of(1990, 1, 1), "NZ", null);

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.TITLE.key(), report.errors().get(0).fieldKey());
    }

    void blankTitleIsRejected() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "davis", LocalDate.of(1990, 1, 1), "NZ", " ");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.TITLE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullNationalityIsRejected() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "davis", LocalDate.of(1990, 1, 1), null, "Ms");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.NATIONALITY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankNationalityIsRejected() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "davis", LocalDate.of(1990, 1, 1), "  ", "Ms");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(CustomerField.NATIONALITY.key(), report.errors().get(0).fieldKey());
    }


    @Test
    void bothInputsIsRejected() {
        // Arrange: build the input
        CustomerInput input = new CustomerInput("Ada", "  ", LocalDate.now().plusDays(1), "NZ", "Ms");

        // Act: call the method
        ValidationReport report = service.validateCustomer(input, true);

        // Assert: check the result
        assertEquals(2, report.errors().size());
        assertEquals(CustomerField.FAMILY_NAME.key(), report.errors().get(0).fieldKey());
        assertEquals(CustomerField.DATE_OF_BIRTH.key(), report.errors().get(1).fieldKey());
    }
}