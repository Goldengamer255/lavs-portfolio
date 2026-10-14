package lavs.validation;

import lavs.service.*;

import java.util.List;

// AI-assisted: class and method signatures influenced by AI; bodies and TODO notes written by me.
public class ValidationService {

    public ValidationReport validateCustomer(CustomerInput input, boolean creating) {
        // TODO: Checks that the required family name is present and the date of birth is not in the future
        return new ValidationReport(List.of());
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