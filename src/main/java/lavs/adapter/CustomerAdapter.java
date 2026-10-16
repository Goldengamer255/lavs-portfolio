package lavs.adapter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
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
            formatDate(input.dateOfBirth()),
            input.familyName(),
            input.firstName(),
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
    var message = new mainframe.client.messages.customer.Search(page, nameQuery, size);

    try {
      Status status = message.send(connection);

      if (!status.getWasSuccessful()) {
        return OperationResult.failure(
            status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
      }

      var customers = new ArrayList<CustomerSummary>();
      for (var item : message.getItems()) {
        customers.add(
            new CustomerSummary(
                item.getNumber(),
                item.getFirstName(),
                item.getFamilyName(),
                parseDate(item.getDateOfBirth()),
                item.getTitle(),
                item.getStatus()));
      }
      var result =
          new Page<CustomerSummary>(customers, message.getPage(), size, message.getTotal());

      return OperationResult.success(result, status.getTransactionId());

    } catch (IOException e) {
      return OperationResult.failure(
          StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
    }
  }

  @Override
  public OperationResult<Customer> update(Customer customer) {
    var message =
        new mainframe.client.messages.customer.Update(
            formatDate(customer.dateOfBirth()),
            customer.familyName(),
            customer.firstName(),
            customer.nationality(),
            customer.number(),
            customer.title());
    try {
      Status status = message.send(connection);

      if (!status.getWasSuccessful()) {
        return OperationResult.failure(
            status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
      }
      return OperationResult.success(
          new Customer(
              message.getNumber(),
              message.getFirstName(),
              message.getFamilyName(),
              parseDate(message.getDateOfBirth()),
              message.getNationality(),
              message.getTitle(),
              message.getStatus()),
          status.getTransactionId());

    } catch (IOException e) {
      return OperationResult.failure(
          StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
    }
  }

  @Override
  public OperationResult<String> changeStatus(String customerNumber, String status) {
    var message = new mainframe.client.messages.customer.UpdateStatus(customerNumber, status);
    try {
      Status result = message.send(connection);

      if (!result.getWasSuccessful()) {
        return OperationResult.failure(
            result.getErrorCode(), result.getErrorMessage(), result.getTransactionId());
      }
      return OperationResult.success(message.getCustomer(), result.getTransactionId());

    } catch (IOException e) {
      return OperationResult.failure(
          StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
    }
  }

  // Address related methods
  @Override
  public OperationResult<Address> addAddress(Address address) {}

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
