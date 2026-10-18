package lavs.validation;

import lavs.service.AddressInput;
import lavs.service.CustomerInput;
import lavs.service.EmailInput;
import lavs.service.EmployerInput;
import lavs.service.LoanInput;
import lavs.service.PaymentInput;
import lavs.service.PhoneInput;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;



// AI-assisted: class and method signatures influenced by AI; All bodies written by me.
/**
 * Checks form input before it is sent to the mainframe.
 *
 * <p>Each validate method returns a {@link ValidationReport} listing every problem found,
 * with each issue tied to the field it belongs to so the screen can outline the right box.
 * An empty report means the input is valid. These methods never call the mainframe.</p>
 */
public class ValidationService {

    public ValidationReport validateCustomer(CustomerInput input, boolean creating) {
        // Required: first name, family name, nationality, title, Date of Birth. Date of birth must not be in the future.
        List<ValidationIssue> issues = new ArrayList<>();

        if (input.firstName() == null || input.firstName().isBlank()) {
            issues.add(new ValidationIssue(CustomerField.FIRST_NAME.key(), "First Name is required"));
        }

        if (input.familyName() == null || input.familyName().isBlank()) {
            issues.add(new ValidationIssue(CustomerField.FAMILY_NAME.key(), "Family Name is required"));
        }

        if (input.dateOfBirth() == null) {
            issues.add(new ValidationIssue(CustomerField.DATE_OF_BIRTH.key(), "Date of birth is required"));
        } else if (input.dateOfBirth().isAfter(LocalDate.now())) {
            issues.add(new ValidationIssue(CustomerField.DATE_OF_BIRTH.key(), "Date of birth cannot be in the future"));
        }

        if (input.nationality() == null || input.nationality().isBlank()) {
            issues.add(new ValidationIssue(CustomerField.NATIONALITY.key(), "Nationality is required"));
        }

        if (input.title() == null || input.title().isBlank()) {
            issues.add(new ValidationIssue(CustomerField.TITLE.key(), "Title is required"));
        }

        return new ValidationReport(issues);
    }

    public ValidationReport validateAddress(AddressInput input) {
        // Checks that the address is in the form line1, city, country, type, otherType if needed
        List<ValidationIssue> issues = new ArrayList<>();
        if (input.line1() == null || input.line1().isBlank()) {
            issues.add(new ValidationIssue(AddressField.LINE1.key(), "Line1 is required"));
        }

        if (input.city() == null || input.city().isBlank()) {
            issues.add(new ValidationIssue(AddressField.CITY.key(), "City is required"));
        }

        if (input.country() == null || input.country().isBlank()) {
            issues.add(new ValidationIssue(AddressField.COUNTRY.key(), "Country is required"));
        }

        if (input.type() == null || input.type().isBlank()) {
            issues.add(new ValidationIssue(AddressField.TYPE.key(), "Type of Address is required"));
        }

        if ("Other".equals(input.type()) && (input.otherType() == null || input.otherType().isBlank())) {
            issues.add(new ValidationIssue(AddressField.OTHER_TYPE.key(), "Other type is required when type of Address is other"));
        }

        return new ValidationReport(issues);
    }

    // AI-assisted: email format regex given by AI; rest of method written by me.
    public ValidationReport validateEmail(EmailInput input) {
        // Checks if email is in valid format and has a valid email address
        List<ValidationIssue> issues = new ArrayList<>();

        if (input.address() == null || input.address().isBlank()) {
            issues.add(new ValidationIssue(EmailField.ADDRESS.key(), "Address is required"));
        }

        else if (!input.address().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            issues.add(new ValidationIssue(EmailField.ADDRESS.key(), "Email address is not a valid email"));
        }

        if ( "Other".equals(input.type()) && (input.otherType() == null || input.otherType().isBlank())) {
            issues.add(new ValidationIssue(EmailField.OTHER_TYPE.key(), "Other type is required field when the email type is other"));
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
            issues.add(new ValidationIssue(PhoneField.COUNTRY_CODE.key(), "Country code is required"));
        } else if (!input.countryCode().matches("\\+\\d{1,3}")) {
            issues.add(new ValidationIssue(PhoneField.COUNTRY_CODE.key(), "Country code is not valid"));
        }

        if (input.number() == null || input.number().isBlank()) {
            issues.add(new ValidationIssue(PhoneField.NUMBER.key(), "Number is required"));
        } else if (!input.number().matches("[0-9\\s-]+")) {
            issues.add(new ValidationIssue(PhoneField.NUMBER.key(), "Number is not a valid number"));
        } else if (!input.number().matches(".*\\d.*")) {
            issues.add(new ValidationIssue(PhoneField.NUMBER.key(), "Number must contain at least one digit"));
        }

        if (input.type() == null || input.type().isBlank()) {
            issues.add(new ValidationIssue(PhoneField.TYPE.key(), "Type of number is required"));
        }

        if ("Other".equals(input.type())  && (input.otherType() == null || input.otherType().isBlank())) {
            issues.add(new ValidationIssue(PhoneField.OTHER_TYPE.key(), "Other type is required if type of number is other"));
        }

        return new ValidationReport(issues);
    }

    // AI-assisted: salary regex from AI; rest of method written by me.
    public ValidationReport validateEmployer(EmployerInput input) {
        // All fields required
        List<ValidationIssue> issues = new ArrayList<>();

        if (input.name() == null || input.name().isBlank()) {
            issues.add(new ValidationIssue(EmployerField.NAME.key(), "Name is required"));
        }
        if (input.position() == null || input.position().isBlank()) {
            issues.add(new ValidationIssue(EmployerField.POSITION.key(), "Position is required"));
        }
        if (input.salary() == null || input.salary().isBlank()) {
            issues.add(new ValidationIssue(EmployerField.SALARY.key(), "Salary is required"));
        } else {
            BigDecimal salary = parseNumber(input.salary());
            if (salary == null || salary.signum() < 0 || salary.scale() > 2) {
                issues.add(new ValidationIssue(EmployerField.SALARY.key(), "Salary must be a positive number"));
            }
        }
        if (input.type() == null || input.type().isBlank()) {
            issues.add(new ValidationIssue(EmployerField.TYPE.key(), "Type is required"));
        }

        return new ValidationReport(issues);
    }

    private static BigDecimal parseNumber(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // AI-assisted: parseNumber helper and BigDecimal approach for rate and payment fields influenced by AI; rest of method written by me.
    public ValidationReport validateLoan(LoanInput input) {
        //The principal and payment must be greater than 0, frequencies required, and renewal date after start date
        List<ValidationIssue> issues = new ArrayList<>();

        if (input.principal() == null || input.principal().isBlank()) {
            issues.add(new ValidationIssue(LoanField.PRINCIPAL.key(), "Principal is required"));
        } else {
            BigDecimal value = parseNumber(input.principal());
            if (value == null) {
                issues.add(new ValidationIssue(LoanField.PRINCIPAL.key(), "Principal must be a number"));
            } else if (value.signum() <= 0) {
                issues.add(new ValidationIssue(LoanField.PRINCIPAL.key(), "Principal must be greater than 0"));
            }
        }

        if (input.paymentAmount() == null || input.paymentAmount().isBlank()) {
            issues.add(new ValidationIssue(LoanField.PAYMENT_AMOUNT.key(), "Payment amount is required"));
        } else {
            BigDecimal value = parseNumber(input.paymentAmount());
            if (value == null) {
                issues.add(new ValidationIssue(LoanField.PAYMENT_AMOUNT.key(), "Payment amount must be a number"));
            } else if (value.signum() <= 0) {
                issues.add(new ValidationIssue(LoanField.PAYMENT_AMOUNT.key(), "Payment amount must be greater than 0"));
            }
        }

        if (input.rateType() == null || input.rateType().isBlank()) {
            issues.add(new ValidationIssue(LoanField.RATE_TYPE.key(), "Rate type is required"));
        }

        if (input.rateAmount() == null || input.rateAmount().isBlank()) {
            issues.add(new ValidationIssue(LoanField.RATE_AMOUNT.key(), "Rate amount is required"));
        } else {
            BigDecimal value = parseNumber(input.rateAmount());
            if (value == null) {
                issues.add(new ValidationIssue(LoanField.RATE_AMOUNT.key(), "Rate amount must be a number"));
            } else if (value.signum() < 0) {
                issues.add(new ValidationIssue(LoanField.RATE_AMOUNT.key(), "Rate amount must not be negative"));
            }
        }

        if (input.rateFrequency() == null || input.rateFrequency().isBlank()) {
            issues.add(new ValidationIssue(LoanField.RATE_FREQUENCY.key(), "Rate frequency is required"));
        }

        if (input.paymentFrequency() == null || input.paymentFrequency().isBlank()) {
            issues.add(new ValidationIssue(LoanField.PAYMENT_FREQUENCY.key(), "Payment frequency is required"));
        }

        if (input.startDate() == null) {
            issues.add(new ValidationIssue(LoanField.START_DATE.key(), "Start date is required"));
        }
        if (input.renewalDate() == null) {
            issues.add(new ValidationIssue(LoanField.RENEWAL_DATE.key(), "Renewal date is required"));
        } else if (input.startDate() != null && !input.renewalDate().isAfter(input.startDate())) {
            issues.add(new ValidationIssue(LoanField.RENEWAL_DATE.key(), "Renewal date must be after the start date"));
        }

        return new ValidationReport(issues);
    }

    // AI-assisted: rules and structure influenced by AI; adapted from validateLoan; body written by me.
    public ValidationReport validatePayment(PaymentInput input) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (input.amount() == null || input.amount().isBlank()) {
            issues.add(new ValidationIssue(PaymentField.AMOUNT.key(), "Amount is required"));
        } else {
            BigDecimal value = parseNumber(input.amount());
            if (value == null) {
                issues.add(new ValidationIssue(PaymentField.AMOUNT.key(), "Amount must be a number"));
            } else if (value.signum() <= 0) {
                issues.add(new ValidationIssue(PaymentField.AMOUNT.key(), "Amount must be greater than 0"));
            }
        }

        if (input.date() == null) {
            issues.add(new ValidationIssue(PaymentField.DATE.key(), "Date is required"));
        } else if (input.date().isAfter(LocalDate.now())) {
            issues.add(new ValidationIssue(PaymentField.DATE.key(), "Payment date cannot be in the future"));
        }

        return new ValidationReport(issues);
    }
}
