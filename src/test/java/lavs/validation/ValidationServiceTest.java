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

    // VALIDATE ADDRESS TESTS
    @Test
    void nullCityIsRejected() {
        // Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", null, "NZ", "1010", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validateAddress(input);

        // 3. Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.CITY.key(), report.errors().get(0).fieldKey());

    }

    @Test
    void validAddressIsAccepted() {
        // Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", "NZ", "1010", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validateAddress(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void nullLine1IsRejected() {
        // Arrange: build the input
        AddressInput input = new AddressInput(null, "ontario", "Central", "mexico", "NZ", "1010", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validateAddress(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.LINE1.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullCountryIsRejected() {
        // Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", null , "1010", "Home", null, true, true);

        // Act: call the method
        ValidationReport report = service.validateAddress(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.COUNTRY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullTypeIsRejected() {
        // Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", "space" , "1010", null, null, true, true);

        // Act: call the method
        ValidationReport report = service.validateAddress(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void otherNoTypeRejected() {
        // Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", "space" , "1010", "Other", null, true, true);

        // Act: call the method
        ValidationReport report = service.validateAddress(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(AddressField.OTHER_TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void otherWithTypeAccepted() {
        // Arrange: build the input
        AddressInput input = new AddressInput("1 Queen St", "ontario", "Central", "mexico", "space" , "1010", "Other", "Indigenous", true, true);

        // Act: call the method
        ValidationReport report = service.validateAddress(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void multipleErrorsAreRejected() {
        // Arrange: build the input
        AddressInput input = new AddressInput(null, "ontario", "Central", null, null , "1010", "Other", "Indigenous", true, true);

        // Act: call the method
        ValidationReport report = service.validateAddress(input);

        // Assert: check the result
        assertEquals(3, report.errors().size());
        assertEquals(AddressField.LINE1.key(), report.errors().get(0).fieldKey());
        assertEquals(AddressField.CITY.key(), report.errors().get(1).fieldKey());
        assertEquals(AddressField.COUNTRY.key(), report.errors().get(2).fieldKey());
    }

    //EMAIL VALIDATION
    @Test
    void validEmailIsAccepted() {
        // Arrange: build the input
        EmailInput input = new EmailInput("tanush@example.com", "Work", null, true);

        // Act: call the method
        ValidationReport report = service.validateEmail(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void nullEmailIsRejected() {
        // Arrange: build the input
        EmailInput input = new EmailInput(null, "Work", null, true);

        // Act: call the method
        ValidationReport report = service.validateEmail(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.ADDRESS.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void missingTypeEmailIsRejected() {
        // Arrange: build the input
        EmailInput input = new EmailInput("tp@gmail.com", null, null, true);

        // Act: call the method
        ValidationReport report = service.validateEmail(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void otherWithNoOtherTypeEmailIsRejected() {
        // Arrange: build the input
        EmailInput input = new EmailInput("tp@gmail.com", "Other", null, true);

        // Act: call the method
        ValidationReport report = service.validateEmail(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.OTHER_TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void multipleErrorsEmailIsRejected() {
        // Arrange: build the input
        EmailInput input = new EmailInput(null, null, null, true);

        // Act: call the method
        ValidationReport report = service.validateEmail(input);

        // Assert: check the result
        assertEquals(2, report.errors().size());
        assertEquals(EmailField.ADDRESS.key(), report.errors().get(0).fieldKey());
        assertEquals(EmailField.TYPE.key(), report.errors().get(1).fieldKey());
    }

    @Test
    void blankEmailIsRejected() {
        // Arrange: build the input
        EmailInput input = new EmailInput(" ", "Work", null, true);

        // Act: call the method
        ValidationReport report = service.validateEmail(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.ADDRESS.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankTypeIsRejected() {
        // Arrange: build the input
        EmailInput input = new EmailInput("fda@gmail.com", " ", null, true);

        // Act: call the method
        ValidationReport report = service.validateEmail(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void badFormatEmailIsRejected() {
        // Arrange: build the input
        EmailInput input = new EmailInput("ash", "Work", null, true);

        // Act: call the method
        ValidationReport report = service.validateEmail(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmailField.ADDRESS.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void typeOtherWithOtherIsAccepted() {
        // Arrange: build the input
        EmailInput input = new EmailInput("ash@gmail.com", "Other", "Personal", true);

        // Act: call the method
        ValidationReport report = service.validateEmail(input);

        // Assert: check the result
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

    //Validation EmployerInput
    @Test
    void validEmployerIsAccepted() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Bathroom", "500000", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void validEmployerWithDecimalSalaryIsAccepted() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Bathroom", "500000.10", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void negativeSalaryIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Bathroom", "-500000.10", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.SALARY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankSalaryIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Bathroom", " ", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.SALARY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nonNumberSalaryIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Bathroom", "abc", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.SALARY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void decimalNumberSalaryIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Bathroom", "11.0231", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.SALARY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void regressionEmployerIsAccepted() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Bathroom", "1.0E7", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void nullNameIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput(null, "Bathroom", "500000", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.NAME.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullPositionIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", null, "500000", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.POSITION.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankNameIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput(" ", "Bathroom", "500000", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.NAME.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankPositionIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", " ", "500000", "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.POSITION.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullSalaryIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Waiter", null, "Primary");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.SALARY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullEmployerTypeIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Artist", "500000", null);

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankEmployerTypeIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput("McDonalds", "Artist", "500000", " ");

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(EmployerField.TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void multipleEmployerErrorsIsRejected() {
        // Arrange: build the input
        EmployerInput input = new EmployerInput(null, null, "-500000", null);

        // Act: call the method
        ValidationReport report = service.validateEmployer(input);

        // Assert: check the result
        assertEquals(4, report.errors().size());
        assertEquals(EmployerField.NAME.key(), report.errors().get(0).fieldKey());
        assertEquals(EmployerField.POSITION.key(), report.errors().get(1).fieldKey());
        assertEquals(EmployerField.SALARY.key(), report.errors().get(2).fieldKey());
        assertEquals(EmployerField.TYPE.key(), report.errors().get(3).fieldKey());
    }

    //VALIDATE LOAN TESTS
    @Test
    void validLoanIsAccepted() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "3000", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void nullPrincipalIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput(null, "3000", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PRINCIPAL.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankPrincipalIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput(" ", "3000", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PRINCIPAL.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void letterPrincipalIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("abc", "3000", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PRINCIPAL.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void negativePrincipalIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("-100000", "3000", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PRINCIPAL.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void zeroPrincipalIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("0", "3000", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PRINCIPAL.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullPaymentIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", null, "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PAYMENT_AMOUNT.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void blankPaymentIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", " ", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PAYMENT_AMOUNT.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void letterPaymentIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "abc", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PAYMENT_AMOUNT.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void negativePaymentIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "-100", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PAYMENT_AMOUNT.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void zeroPaymentIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "0", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PAYMENT_AMOUNT.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void negativeRateIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "-6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.RATE_AMOUNT.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void letterRateIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "abc", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.RATE_AMOUNT.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void zeroRateIsAccepted() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "0", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(0, report.errors().size());
    }

    @Test
    void nullRateTypeIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "6.4", null, "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.RATE_TYPE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullRateFrequencyIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "6.4", "Fixed", null, "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.RATE_FREQUENCY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullPaymentFrequencyIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "6.4", "Fixed", "Yearly", null, LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.PAYMENT_FREQUENCY.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullRateAmountIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", null, "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.RATE_AMOUNT.key(), report.errors().get(0).fieldKey());
    }



    @Test
    void nullStartDateIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "6.4", "Fixed", "Yearly", "Monthly", null, LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.START_DATE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void nullRenewDateIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), null);

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.RENEWAL_DATE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void bothNullDateIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "6.4", "Fixed", "Yearly", "Monthly", null, null);

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(2, report.errors().size());
        assertEquals(LoanField.START_DATE.key(), report.errors().get(0).fieldKey());
        assertEquals(LoanField.RENEWAL_DATE.key(), report.errors().get(1).fieldKey());
    }

    @Test
    void SameDatesIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.RENEWAL_DATE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void renewBeforeStartDateIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput("10000", "100", "6.4", "Fixed", "Yearly", "Monthly", LocalDate.of(2026, 5, 2), LocalDate.of(2026, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(1, report.errors().size());
        assertEquals(LoanField.RENEWAL_DATE.key(), report.errors().get(0).fieldKey());
    }

    @Test
    void multipleLoanErrorsIsRejected() {
        // Arrange: build the input
        LoanInput input = new LoanInput(null, null, "6.4", "Fixed", "Yearly", "Monthly", null, LocalDate.of(2027, 2, 2));

        // Act: call the method
        ValidationReport report = service.validateLoan(input);

        // Assert: check the result
        assertEquals(3, report.errors().size());
        assertEquals(LoanField.PRINCIPAL.key(), report.errors().get(0).fieldKey());
        assertEquals(LoanField.PAYMENT_AMOUNT.key(), report.errors().get(1).fieldKey());
        assertEquals(LoanField.START_DATE.key(), report.errors().get(2).fieldKey());
    }
}