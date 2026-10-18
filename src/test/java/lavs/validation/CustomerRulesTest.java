package lavs.validation;

import lavs.domain.Address;
import lavs.domain.Email;
import lavs.domain.Phone;
import lavs.service.ValidationReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerRulesTest {

    private final CustomerRules rules = new CustomerRules();

    // AI-assisted: helper methods for building Address, Phone test structure written with AI help.
    private Address address(long id, boolean mailing) {
        return new Address(id, null, null, null, null, null, null, null, null, null, mailing, false);
    }

    private Phone phone(long id) {
        return new Phone(id, false, null, null, null, false, null, null, null);
    }

    private Email email(long id) {
        return new Email(id, null, null, null, false, null, null);
    }

    @Test
    public void mailingAddressWithPhoneIsAccepted() {
        // Arrange
        List<Address> addresses = List.of(address(1, true));
        List<Phone> phones = List.of(phone(1));

        // Act
        ValidationReport report = rules.checkLoanReadiness(addresses, phones, List.of());

        // Assert
        assertEquals(0, report.errors().size());
    }

    @Test
    public void mailingAddressWithEmailIsAccepted() {
        // Arrange
        List<Address> addresses = List.of(address(1, true));
        List<Email> emails = List.of(email(1));

        // Act
        ValidationReport report = rules.checkLoanReadiness(addresses, List.of(), emails);

        // Assert
        assertEquals(0, report.errors().size());
    }

    @Test
    public void noMailingAddressIsRejected() {
        // Arrange
        List<Address> addresses = List.of(address(1, false));
        List<Phone> phones = List.of(phone(1));

        // Act
        ValidationReport report = rules.checkLoanReadiness(addresses, phones, List.of());

        // Assert
        assertEquals(1, report.errors().size());
        assertEquals(FieldKey.GLOBAL, report.errors().get(0).fieldKey());
    }

    @Test
    public void noContactsIsRejected() {
        // Arrange
        List<Address> addresses = List.of(address(1, true));

        // Act
        ValidationReport report = rules.checkLoanReadiness(addresses, List.of(), List.of());

        // Assert
        assertEquals(1, report.errors().size());
        assertEquals(FieldKey.GLOBAL, report.errors().get(0).fieldKey());
    }

    @Test
    public void noMailingAddressAndNoContactsIsRejected() {
        // Arrange
        List<Address> addresses = List.of(address(1, false));

        // Act
        ValidationReport report = rules.checkLoanReadiness(addresses, List.of(), List.of());

        // Assert
        assertEquals(2, report.errors().size());
        assertEquals(FieldKey.GLOBAL, report.errors().get(0).fieldKey());
        assertEquals(FieldKey.GLOBAL, report.errors().get(1).fieldKey());
        assertEquals("A mailing address is required", report.errors().get(0).message());
    }

    //DELETE TESTS
    @Test
    public void onlyMailingAddressDeleteIsBlocked() {
        // Arrange
        List<Address> addresses = List.of(address(1, true));

        // Act
        DeleteCheck check = rules.canDeleteAddress(addresses, 1);

        // Assert
        assertFalse(check.allowed());
        assertNotNull(check.reason());
    }

    @Test
    public void twoMailingAddressDeleteIsAllowed() {
        // Arrange
        List<Address> addresses = List.of(address(1, true), address(2, true));

        // Act
        DeleteCheck check = rules.canDeleteAddress(addresses, 1);

        // Assert
        assertTrue(check.allowed());
        assertNull(check.reason());
    }

    @Test
    public void nonMailingAddressDeleteIsAllowed() {
        // Arrange
        List<Address> addresses = List.of(address(1, true), address(2, false));

        // Act
        DeleteCheck check = rules.canDeleteAddress(addresses, 2);

        // Assert
        assertTrue(check.allowed());
        assertNull(check.reason());
    }

    @Test
    public void unknownAddressDeleteIsAllowed() {
        // Arrange
        List<Address> addresses = List.of(address(1, true));

        // Act
        DeleteCheck check = rules.canDeleteAddress(addresses, 99);

        // Assert
        assertTrue(check.allowed());
        assertNull(check.reason());
    }

    @Test
    public void onlyPhoneDeleteIsBlocked() {
        // Arrange
        List<Phone> phones = List.of(phone(1));
        List<Email> emails = List.of();

        // Act
        DeleteCheck check = rules.canDeletePhone(phones, emails, 1);

        // Assert
        assertFalse(check.allowed());
        assertNotNull(check.reason());
    }

    @Test
    public void phoneWithEmailDeleteIsAllowed() {
        // Arrange
        List<Phone> phones = List.of(phone(1));
        List<Email> emails = List.of(email(1));

        // Act
        DeleteCheck check = rules.canDeletePhone(phones, emails, 1);

        // Assert
        assertTrue(check.allowed());
        assertNull(check.reason());
    }

    @Test
    public void twoPhonesDeleteIsAllowed() {
        // Arrange
        List<Phone> phones = List.of(phone(1), phone(2));
        List<Email> emails = List.of();

        // Act
        DeleteCheck check = rules.canDeletePhone(phones, emails, 1);

        // Assert
        assertTrue(check.allowed());
        assertNull(check.reason());
    }

    @Test
    public void unknownPhoneDeleteIsAllowed() {
        // Arrange
        List<Phone> phones = List.of(phone(1));
        List<Email> emails = List.of();

        // Act
        DeleteCheck check = rules.canDeletePhone(phones, emails, 99);

        // Assert
        assertTrue(check.allowed());
        assertNull(check.reason());
    }

    @Test
    public void onlyEmailDeleteIsBlocked() {
        // Arrange
        List<Phone> phones = List.of();
        List<Email> emails = List.of(email(1));

        // Act
        DeleteCheck check = rules.canDeleteEmail(phones, emails, 1);

        // Assert
        assertFalse(check.allowed());
        assertNotNull(check.reason());
    }

    @Test
    public void emailWithPhoneDeleteIsAllowed() {
        // Arrange
        List<Phone> phones = List.of(phone(1));
        List<Email> emails = List.of(email(1));

        // Act
        DeleteCheck check = rules.canDeleteEmail(phones, emails, 1);

        // Assert
        assertTrue(check.allowed());
        assertNull(check.reason());
    }

    @Test
    public void unknownEmailDeleteIsAllowed() {
        // Arrange
        List<Phone> phones = List.of();
        List<Email> emails = List.of(email(1));

        // Act
        DeleteCheck check = rules.canDeleteEmail(phones, emails, 99);

        // Assert
        assertTrue(check.allowed());
        assertNull(check.reason());
    }

    @Test
    public void mailingAddressNextToNonMailingDeleteIsBlocked() {
        // Arrange: the only mailing address sits beside a non-mailing one
        List<Address> addresses = List.of(address(1, true), address(2, false));

        // Act
        DeleteCheck check = rules.canDeleteAddress(addresses, 1);

        // Assert
        assertFalse(check.allowed());
        assertNotNull(check.reason());
    }
}
