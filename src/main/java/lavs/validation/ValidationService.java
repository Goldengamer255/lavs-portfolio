package lavs.validation;

import lavs.domain.Customer;
import lavs.service.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;



// AI-assisted: class and method signatures influenced by AI; All bodies and TODO notes written by me.
public class ValidationService {

    public ValidationReport validateCustomer(CustomerInput input, boolean creating) {
        // Required: first name, family name, nationality, title. Date of birth must not be in the future.
        List<ValidationIssue> issues = new ArrayList<>();

        if (input.firstName() == null || input.firstName().isBlank()) {
            issues.add(new ValidationIssue(CustomerField.FIRST_NAME.key(), "firstName is required"));
        }

        if (input.familyName() == null || input.familyName().isBlank()) {
            issues.add(new ValidationIssue(CustomerField.FAMILY_NAME.key(), "familyName is required"));
        }

        if (input.dateOfBirth() != null && input.dateOfBirth().isAfter(LocalDate.now())) {
            issues.add(new ValidationIssue(CustomerField.DATE_OF_BIRTH.key(), "date of birth cannot be in the future"));
        }

        if (input.nationality() == null || input.nationality().isBlank()) {
            issues.add(new ValidationIssue(CustomerField.NATIONALITY.key(), "nationality is required"));
        }

        if (input.title() == null || input.title().isBlank()) {
            issues.add(new ValidationIssue(CustomerField.TITLE.key(), "title is required"));
        }

        return new ValidationReport(issues);
    }

    public ValidationReport validateAddress(AddressInput input) {
        // TODO: Checks that the address is in the form line1, city, country, type, otherType if needed
        return new ValidationReport(List.of());
    }

    public ValidationReport validateEmail(EmailInput input) {
        // TODO: Checks if email is in valid format and has a valid email address
        return new ValidationReport(List.of());
    }

    public ValidationReport validatePhone(PhoneInput input) {
        //TODO: Checks if the phone number is in numbers only and digits only, and other type is used when needed
        return new ValidationReport(List.of());
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