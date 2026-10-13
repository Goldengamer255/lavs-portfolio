package lavs.validation;

import lavs.domain.Address;
import lavs.domain.Email;
import lavs.domain.Phone;
import lavs.service.ValidationReport;

import java.util.List;

// AI-assisted: class and method signatures influenced by AI; bodies and TODO notes written by me.
public class CustomerRules {
    public ValidationReport checkLoanReadiness(List<Address> addresses, List<Phone> phones, List<Email> emails) {
        //TODO: Checks at least one mailing address is present and at least one phone or email
        return new ValidationReport(List.of());
    }

    public DeleteCheck canDeleteAddress(List<Address> addresses, long addressId) {
        // TODO: this method must block deleting the last mailing address
        return new DeleteCheck(true, null);
    }

    public DeleteCheck canDeletePhone(List<Phone> phones, List<Email> emails, long phoneId) {
        // TODO: this method must block deleting the last contact method (phone or email)
        return new DeleteCheck(true, null);
    }

    public DeleteCheck canDeleteEmail(List<Phone> phones, List<Email> emails, long emailId) {
        // TODO: this method must block deleting the last contact method (phone or email)
        return new DeleteCheck(true, null);
    }
}