package lavs.validation;

import lavs.domain.Address;
import lavs.domain.Email;
import lavs.domain.Phone;
import lavs.service.ValidationIssue;
import lavs.service.ValidationReport;

import java.util.ArrayList;
import java.util.List;

// AI-assisted: class and method signatures influenced by AI; bodies written by me.
/**
 * Business rules for a customer's contact details.
 *
 * <p>Checks that a customer is ready for a loan (a mailing address plus a phone number or
 * email) and blocks deleting the last mailing address or the last contact method. Phones
 * and emails are counted together as contact methods.</p>
 */
public class CustomerRules {
    public ValidationReport checkLoanReadiness(List<Address> addresses, List<Phone> phones, List<Email> emails) {
        // Checks at least one mailing address is present and at least one phone or email
        List<ValidationIssue> issues = new ArrayList<>();

        boolean hasMailing = false;

        for (Address address : addresses) {
            if (address.isMailing()) {
                hasMailing = true;
                break;
            }
        }

        if (!hasMailing) {
            issues.add(new ValidationIssue(FieldKey.GLOBAL, "A mailing address is required"));
        }

        if (phones.isEmpty() && emails.isEmpty()) {
            issues.add(new ValidationIssue(FieldKey.GLOBAL, "At least one phone number or email is required"));
        }

        return new ValidationReport(issues);
    }

    public DeleteCheck canDeleteAddress(List<Address> addresses, long addressId) {
        // This method must block deleting the last mailing address

        Address target = null;

        for (Address address : addresses) {
            if (addressId == address.id()) {
                target = address;
            }
        }

        if (target == null) {
            return new DeleteCheck(true, null);
        }

        if (!target.isMailing()) {
            return new DeleteCheck(true, null);
        }

        int mailingCount = 0;
        for (Address address : addresses) {
            if (address.isMailing()) {
                mailingCount++;
            }
        }

        if (mailingCount == 1) {
            return new DeleteCheck(false, "Customer must have at least one mailing address");
        }

        return new DeleteCheck(true, null);
    }

    public DeleteCheck canDeletePhone(List<Phone> phones, List<Email> emails, long phoneId) {
        // This method must block deleting the last contact method (phone or email)

        boolean found = false;
        int total = phones.size() + emails.size();

        for (Phone phone : phones) {
            if (phoneId == phone.id()) {
                found = true;
                break;
            }
        }

        if (!found) {
            return new DeleteCheck(true, null);
        }

        if (total == 1) {
            return new DeleteCheck(false, "Customer must have at least one phone number or email");
        }

        return new DeleteCheck(true, null);
    }

    public DeleteCheck canDeleteEmail(List<Phone> phones, List<Email> emails, long emailId) {
        // This method must block deleting the last contact method (phone or email)

        boolean found = false;
        int total = phones.size() + emails.size();

        for (Email email : emails) {
            if (emailId == email.id()) {
                found = true;
                break;
            }
        }

        if (!found) {
            return new DeleteCheck(true, null);
        }

        if (total == 1) {
            return new DeleteCheck(false, "Customer must have at least one phone number or email");
        }

        return new DeleteCheck(true, null);
    }
}
