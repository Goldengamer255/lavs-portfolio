package lavs.validation;

import lavs.service.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;



// AI-assisted: class and method signatures influenced by AI; bodies and TODO notes written by me.
public class ValidationService {

    public ValidationReport validateCustomer(CustomerInput input, boolean creating) {
        // Checks that the required family name is present and the date of birth is not in the future
        List<ValidationIssue> issues = new ArrayList<>();
        if (input.familyName() == null || input.familyName().isBlank()) {
            issues.add(new ValidationIssue(CustomerField.FAMILY_NAME.key(), "familyName is required"));
        }

        if (input.dateOfBirth() != null && input.dateOfBirth().isAfter(LocalDate.now())) {
            issues.add(new ValidationIssue(CustomerField.DATE_OF_BIRTH.key(), "date of birth cannot be in the future"));
        }

        return new ValidationReport(issues);
    }

    public ValidationReport validateAddress(AddressInput input) {
        // Checks that the address is in the form line1, city, country, type, otherType if needed
        List<ValidationIssue> issues = new ArrayList<>();
        if (input.line1() == null || input.line1().isBlank()) {
            issues.add(new ValidationIssue(AddressField.LINE1.key(), "line1 is required"));
        }

        if (input.city() == null || input.city().isBlank()) {
            issues.add(new ValidationIssue(AddressField.CITY.key(), "city is required"));
        }

        if (input.country() == null || input.country().isBlank()) {
            issues.add(new ValidationIssue(AddressField.COUNTRY.key(), "country is required"));
        }

        if (input.type() == null || input.type().isBlank()) {
            issues.add(new ValidationIssue(AddressField.TYPE.key(), "type is required"));
        }

        if ("Other".equals(input.type()) && (input.otherType() == null || input.otherType().isBlank())) {
            issues.add(new ValidationIssue(AddressField.OTHER_TYPE.key(), "otherType is required when type is Other"));
        }

        return new ValidationReport(issues);
    }

    // AI-assisted: email format regex given by AI; rest of method written by me.
    public ValidationReport validateEmail(EmailInput input) {
        // Checks if email is in valid format and has a valid email address
        List<ValidationIssue> issues = new ArrayList<>();

        if (input.address() == null || input.address().isBlank()) {
            issues.add(new ValidationIssue(EmailField.ADDRESS.key(), "address is required"));
        }

        else if (!input.address().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            issues.add(new ValidationIssue(EmailField.ADDRESS.key(), "address is not a valid email"));
        }

        if ( "Other".equals(input.type()) && (input.otherType() == null || input.otherType().isBlank())) {
            issues.add(new ValidationIssue(EmailField.OTHER_TYPE.key(), "otherType is required field when the type is Other"));
        }

        if (input.type() == null || input.type().isBlank()) {
            issues.add(new ValidationIssue(EmailField.TYPE.key(), "type is required"));
        }

        return new ValidationReport(issues);
    }

    // AI-assisted: phone regexes and digit-check regexes from AI; rest of method written by me.
    public ValidationReport validatePhone(PhoneInput input) {
        // Checks if the phone number is in numbers only and digits only, and other type is used when needed
        List<ValidationIssue> issues = new ArrayList<>();

        if (input.countryCode() == null || input.countryCode().isBlank()) {
            issues.add(new ValidationIssue(PhoneField.COUNTRY_CODE.key(), "countryCode is required"));
        } else if (!input.countryCode().matches("\\+\\d{1,3}")) {
            issues.add(new ValidationIssue(PhoneField.COUNTRY_CODE.key(), "countryCode is not valid"));
        }

        if (input.number() == null || input.number().isBlank()) {
            issues.add(new ValidationIssue(PhoneField.NUMBER.key(), "number is required"));
        } else if (!input.number().matches("[0-9\\s-]+")) {
            issues.add(new ValidationIssue(PhoneField.NUMBER.key(), "number is not a valid number"));
        } else if (!input.number().matches(".*\\d.*")) {
            issues.add(new ValidationIssue(PhoneField.NUMBER.key(), "number must contain at least one digit"));
        }

        if (input.type() == null || input.type().isBlank()) {
            issues.add(new ValidationIssue(PhoneField.TYPE.key(), "type is required"));
        }

        if ("Other".equals(input.type())  && (input.otherType() == null || input.otherType().isBlank())) {
            issues.add(new ValidationIssue(PhoneField.OTHER_TYPE.key(), "otherType is required"));
        }

        return new ValidationReport(issues);
    }

    public ValidationReport validateEmployer(EmployerInput input) {
        //TODO: name required unless the customer has no job, and salary must be a number
        return new ValidationReport(List.of());
    }

    public ValidationReport validateLoan(LoanInput input) {
        //TODO: The principal and payment must be greater than 0, frequencies required, and renewal date after start date
        return new ValidationReport(List.of());
    }

}