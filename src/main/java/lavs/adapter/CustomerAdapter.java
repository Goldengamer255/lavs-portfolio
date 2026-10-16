package lavs.adapter;

import java.io.IOException;
import java.time.LocalDate;
import lavs.domain.Address;
import lavs.domain.AddressSummary;
import lavs.domain.Customer;
import lavs.domain.CustomerSummary;
import lavs.domain.Email;
import lavs.domain.EmailSummary;
import lavs.domain.Employer;
import lavs.domain.EmployerSummary;
import lavs.domain.NewCustomer;
import lavs.domain.Page;
import lavs.domain.Phone;
import lavs.domain.PhoneSummary;
import lavs.gateway.CustomerGateway;
import lavs.gateway.OperationResult;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

public class CustomerAdapter implements CustomerGateway {

  private final mainframe.client.Connection connection;

  public CustomerAdapter(mainframe.client.Connection connection) {
    this.connection = connection;
  }

  // helpers for dates
  private LocalDate parseDate(String value) {
    return (value == null || value.isBlank()) ? null : LocalDate.parse(value);
  }

  private String formatDate(LocalDate value) {
    return value == null ? null : value.toString();
  }

  // Customer related methods
  @Override
  public OperationResult<Customer> create(NewCustomer input) {
    final var message =
        new mainframe.client.messages.customer.Create(
            input.firstName(),
            input.familyName(),
            formatDate(input.dateOfBirth()),
            input.nationality(),
            input.organisation(),
            input.title());

    try {
      Status status = message.send(connection);

      if (!status.getWasSuccessful()) {
        return OperationResult.failure(
            status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
      }

      OperationResult<Customer> result =
          OperationResult.success(
              new Customer(
                  message.getNumber(),
                  message.getFirstName(),
                  message.getFamilyName(),
                  parseDate(message.getDateOfBirth()),
                  message.getNationality(),
                  message.getTitle(),
                  message.getStatus()),
              status.getTransactionId());
      return result;
    } catch (IOException e) {
      return OperationResult.failure(
          // Ai assisted for StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode()
          StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
    }
  }

  @Override
  public OperationResult<Customer> retrieve(String customerNumber) {
    var message = new mainframe.client.messages.customer.Retrieve(customerNumber);

    try {
      Status status = message.send(connection);

      if (!status.getWasSuccessful()) {
        return OperationResult.failure(
            status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
      }

      Customer customer =
          new Customer(
              message.getNumber(),
              message.getFirstName(),
              message.getFamilyName(),
              parseDate(message.getDateOfBirth()),
              message.getNationality(),
              message.getTitle(),
              message.getStatus());

      return OperationResult.success(customer, status.getTransactionId());

    } catch (IOException e) {
      return OperationResult.failure(
          StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
    }
  }

  @Override
  public OperationResult<Page<CustomerSummary>> search(String nameQuery, long page, long size) {
    throw new UnsupportedOperationException("Customer search is not implemented");
  }

  @Override
  public OperationResult<Customer> update(Customer customer) {
    throw new UnsupportedOperationException("Customer update is not implemented");
  }

  @Override
  public OperationResult<String> changeStatus(String customerNumber, String status) {
    throw new UnsupportedOperationException("Customer status change is not implemented");
  }

  // Address related methods
  @Override
  public OperationResult<Address> addAddress(Address address) {
    throw new UnsupportedOperationException("Address creation is not implemented");
  }

  @Override
  public OperationResult<Boolean> deleteAddress(String customerNumber, long addressId) {
    throw new UnsupportedOperationException("Address deletion is not implemented");
  }

  @Override
  public OperationResult<Address> updateAddress(Address address) {
    throw new UnsupportedOperationException("Address update is not implemented");
  }

  @Override
  public OperationResult<Page<AddressSummary>> listAddresses(
      String customerNumber, long page, long size) {
    throw new UnsupportedOperationException("Address listing is not implemented");
  }

  @Override
  public OperationResult<Address> retrieveAddress(String customerNumber, long addressId) {
    throw new UnsupportedOperationException("Address retrieval is not implemented");
  }

  // Phone related methods
  @Override
  public OperationResult<Phone> addPhone(Phone phone) {
    throw new UnsupportedOperationException("Phone creation is not implemented");
  }

  @Override
  public OperationResult<Boolean> deletePhone(String customerNumber, long phoneId) {
    throw new UnsupportedOperationException("Phone deletion is not implemented");
  }

  @Override
  public OperationResult<Phone> updatePhone(Phone phone) {
    throw new UnsupportedOperationException("Phone update is not implemented");
  }

  @Override
  public OperationResult<Page<PhoneSummary>> listPhones(
      String customerNumber, long page, long size) {
    throw new UnsupportedOperationException("Phone listing is not implemented");
  }

  @Override
  public OperationResult<Phone> retrievePhone(String customerNumber, long phoneId) {
    throw new UnsupportedOperationException("Phone retrieval is not implemented");
  }

  // Email related methods
  @Override
  public OperationResult<Email> addEmail(Email email) {
    throw new UnsupportedOperationException("Email creation is not implemented");
  }

  @Override
  public OperationResult<Boolean> deleteEmail(String customerNumber, long emailId) {
    throw new UnsupportedOperationException("Email deletion is not implemented");
  }

  @Override
  public OperationResult<Email> updateEmail(Email email) {
    throw new UnsupportedOperationException("Email update is not implemented");
  }

  @Override
  public OperationResult<Page<EmailSummary>> listEmails(
      String customerNumber, long page, long size) {
    throw new UnsupportedOperationException("Email listing is not implemented");
  }

  @Override
  public OperationResult<Email> retrieveEmail(String customerNumber, long emailId) {
    throw new UnsupportedOperationException("Email retrieval is not implemented");
  }

  // Employer related methods

  @Override
  public OperationResult<Employer> addEmployer(Employer employer) {
    throw new UnsupportedOperationException("Employer creation is not implemented");
  }

  @Override
  public OperationResult<Boolean> deleteEmployer(String customerNumber, long employerId) {
    throw new UnsupportedOperationException("Employer deletion is not implemented");
  }

  @Override
  public OperationResult<Employer> updateEmployer(Employer employer) {
    throw new UnsupportedOperationException("Employer update is not implemented");
  }

  @Override
  public OperationResult<Page<EmployerSummary>> listEmployers(
      String customerNumber, long page, long size) {
    throw new UnsupportedOperationException("Employer listing is not implemented");
  }

  @Override
  public OperationResult<Employer> retrieveEmployer(String customerNumber, long employerId) {
    throw new UnsupportedOperationException("Employer retrieval is not implemented");
  }
}
