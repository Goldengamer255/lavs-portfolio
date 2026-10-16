package lavs.validation;

import lavs.service.*;
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

    //VALIDATE PHONE
    @Test
    void validPhoneIsAccepted() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("+63", "1-223-4567", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void validPhoneWithSpacesIsAccepted() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("+63", "1 223 4567", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }


    @Test
    void blankNumberIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("+63", " ", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(PhoneField.NUMBER.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void invalidNumberIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("+63", "abc", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(PhoneField.NUMBER.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullNumberIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("+63", null, "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(PhoneField.NUMBER.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullCountryCodeIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput(null, "1-223-4567", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(PhoneField.COUNTRY_CODE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankCountryCodeIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput(" ", "1-223-4567", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(PhoneField.COUNTRY_CODE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void noPlusCountryCodeIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("64", "1-223-4567", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(PhoneField.COUNTRY_CODE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void missingTypeIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("+64", "1-223-4567", null, null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(PhoneField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void otherWithNoOtherTypeIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("+64", "1-223-4567", "Other", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(PhoneField.OTHER_TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void otherWithOtherTypeIsAccepted() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("+64", "1-223-4567", "Other", "Personal", true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void multipleErrorsIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput(null, "abc", "Other", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(3, report.errors().size());
        assertEquals(PhoneField.COUNTRY_CODE.key(), report.errors().get(0).fieldKey());
        assertEquals(PhoneField.NUMBER.key(), report.errors().get(1).fieldKey());
        assertEquals(PhoneField.OTHER_TYPE.key(), report.errors().get(2).fieldKey());
    }

    // Dashes with no numbers is rejected
    @Test
    void multipleDashNumberIsRejected() {
        // Arrange: build the input
        PhoneInput input = new PhoneInput("+64", "---", "Work", null, true, true);

        // Act: call the method
        ValidationReport report = service.validatePhone(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(PhoneField.NUMBER.key(), report.errors().get(0).fieldKey());
    }

}