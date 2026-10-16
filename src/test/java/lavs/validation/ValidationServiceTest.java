package lavs.validation;

import lavs.service.AddressInput;
import lavs.service.CustomerInput;
import lavs.service.EmailInput;
import lavs.service.ValidationReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationServiceTest {

    private final ValidationService service = new ValidationService();

    // VALIDATE CUSTOMER TESTS
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

    // VALIDATE ADDRESS TESTS
    @Test
    void nullCityIsRejected() {
        // 1. Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", null, "NZ", "1010", "Home", null, true, true);

        // 2. Act: call the method
        ValidationReport report = service.validateAddress(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.CITY.key(), report.errors().get(0).fieldKey());

    }

    @Test
    void validAddressIsAccepted() {
        // 1. Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", "NZ", "1010", "Home", null, true, true);

        // 2. Act: call the method
        ValidationReport report = service.validateAddress(input);

        // 3. Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void nullLine1IsRejected() {
        // 1. Arrange: build the input
        AddressInput input = new AddressInput(null, "ontario", "Central", "mexico", "NZ", "1010", "Home", null, true, true);

        // 2. Act: call the method
        ValidationReport report = service.validateAddress(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.LINE1.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullCountryIsRejected() {
        // 1. Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", null , "1010", "Home", null, true, true);

        // 2. Act: call the method
        ValidationReport report = service.validateAddress(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.COUNTRY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullTypeIsRejected() {
        // 1. Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", "space" , "1010", null, null, true, true);

        // 2. Act: call the method
        ValidationReport report = service.validateAddress(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void otherNoTypeRejected() {
        // 1. Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", "space" , "1010", "Other", null, true, true);

        // 2. Act: call the method
        ValidationReport report = service.validateAddress(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.OTHER_TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void otherWithTypeAccepted() {
        // 1. Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", "space" , "1010", "Other", "Indigenous", true, true);

        // 2. Act: call the method
        ValidationReport report = service.validateAddress(input);

        // 3. Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void multipleErrorsAreRejected() {
        // 1. Arrange: build the input
        AddressInput input = new AddressInput(null, "ontario", "Central", null, null , "1010", "Other", "Indigenous", true, true);

        // 2. Act: call the method
        ValidationReport report = service.validateAddress(input);

        // 3. Assert: check the result
        assertEquals(3, report.errors().size());
        assertEquals(AddressField.LINE1.key(), report.errors().get(0).fieldKey());
        assertEquals(AddressField.CITY.key(), report.errors().get(1).fieldKey());
        assertEquals(AddressField.COUNTRY.key(), report.errors().get(2).fieldKey());
    }

    //EMAIL VALIDATION
    @Test
    void validEmailIsAccepted() {
        // 1. Arrange: build the input
        EmailInput input = new EmailInput("tanush@example.com", "Work", null, true);

        // 2. Act: call the method
        ValidationReport report = service.validateEmail(input);

        // 3. Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void nullEmailIsRejected() {
        // 1. Arrange: build the input
        EmailInput input = new EmailInput(null, "Work", null, true);

        // 2. Act: call the method
        ValidationReport report = service.validateEmail(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.ADDRESS.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void missingTypeEmailIsRejected() {
        // 1. Arrange: build the input
        EmailInput input = new EmailInput("tp@gmail.com", null, null, true);

        // 2. Act: call the method
        ValidationReport report = service.validateEmail(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void otherWithNoOtherTypeEmailIsRejected() {
        // 1. Arrange: build the input
        EmailInput input = new EmailInput("tp@gmail.com", "Other", null, true);

        // 2. Act: call the method
        ValidationReport report = service.validateEmail(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.OTHER_TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void multipleErrorsEmailIsRejected() {
        // 1. Arrange: build the input
        EmailInput input = new EmailInput(null, null, null, true);

        // 2. Act: call the method
        ValidationReport report = service.validateEmail(input);

        // 3. Assert: check the result
        assertEquals(2, report.errors().size());
        assertEquals(EmailField.ADDRESS.key(), report.errors().get(0).fieldKey());
        assertEquals(EmailField.TYPE.key(), report.errors().get(1).fieldKey());
    }

    @Test
    void blankEmailIsRejected() {
        // 1. Arrange: build the input
        EmailInput input = new EmailInput(" ", "Work", null, true);

        // 2. Act: call the method
        ValidationReport report = service.validateEmail(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.ADDRESS.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankTypeIsRejected() {
        // 1. Arrange: build the input
        EmailInput input = new EmailInput("fda@gmail.com", " ", null, true);

        // 2. Act: call the method
        ValidationReport report = service.validateEmail(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void badFormatEmailIsRejected() {
        // 1. Arrange: build the input
        EmailInput input = new EmailInput("ash", "Work", null, true);

        // 2. Act: call the method
        ValidationReport report = service.validateEmail(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.ADDRESS.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void typeOtherWithOtherIsAccepted() {
        // 1. Arrange: build the input
        EmailInput input = new EmailInput("ash@gmail.com", "Other", "Personal", true);

        // 2. Act: call the method
        ValidationReport report = service.validateEmail(input);

        // 3. Assert: check the result
        assertEquals(0, report.errors().size());
    }

}