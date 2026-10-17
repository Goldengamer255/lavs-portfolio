package lavs.adapter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.function.Supplier;
import lavs.domain.Address;
import lavs.domain.AddressSummary;
import lavs.domain.Customer;
import lavs.domain.CustomerSummary;
import lavs.domain.CustomerDocument;
import lavs.domain.CustomerDocumentSummary;
import lavs.domain.DocumentMetadata;
import lavs.domain.Email;
import lavs.domain.EmailSummary;
import lavs.domain.Employer;
import lavs.domain.EmployerSummary;
import lavs.domain.NewCustomer;
import lavs.domain.Page;
import lavs.domain.Phone;
import lavs.domain.PhoneSummary;
import lavs.gateway.CustomerGateway;
import lavs.gateway.DocumentGateway;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

public class CustomerAdapter implements CustomerGateway, DocumentGateway {

  private final Connection connection;

  public CustomerAdapter(Connection connection) {
    this.connection = connection;
  }

  private static LocalDate parseDate(String value) {
    return (value == null || value.isBlank()) ? null : LocalDate.parse(value);
  }

  private static String formatDate(LocalDate value) {
    return value == null ? null : value.toString();
  }

  // AI-assisted: share mainframe status and I/O handling across customer operations.
  private <T> OperationResult<T> send(Message message, Supplier<T> mapResponse) {
    try {
      Status status = message.send(connection);
      if (!status.getWasSuccessful()) {
        return OperationResult.failure(
            status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
      }
      return OperationResult.success(mapResponse.get(), status.getTransactionId());
    } catch (IOException e) {
      return OperationResult.failure(
          StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
    }
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

    return send(
        message,
        () ->
            new Customer(
                message.getNumber(),
                message.getFirstName(),
                message.getFamilyName(),
                parseDate(message.getDateOfBirth()),
                message.getNationality(),
                message.getTitle(),
                message.getStatus()));
  }

  @Override
  public OperationResult<Customer> retrieve(String customerNumber) {
    var message = new mainframe.client.messages.customer.Retrieve(customerNumber);
    return send(
        message,
        () ->
            new Customer(
                message.getNumber(),
                message.getFirstName(),
                message.getFamilyName(),
                parseDate(message.getDateOfBirth()),
                message.getNationality(),
                message.getTitle(),
                message.getStatus()));
  }

  @Override
  public OperationResult<Page<CustomerSummary>> search(String nameQuery, long page, long size) {
    var message = new mainframe.client.messages.customer.Search(page, nameQuery, size);

    return send(message, () -> {
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
      return new Page<>(customers, message.getPage(), size, message.getTotal());
    });
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
    return send(
        message,
        () ->
            new Customer(
                message.getNumber(),
                message.getFirstName(),
                message.getFamilyName(),
                parseDate(message.getDateOfBirth()),
                message.getNationality(),
                message.getTitle(),
                message.getStatus()));
  }

  @Override
  public OperationResult<String> changeStatus(String customerNumber, String status) {
    var message = new mainframe.client.messages.customer.UpdateStatus(customerNumber, status);
    return send(message, message::getStatus);
  }

  // Address related methods
  @Override
  public OperationResult<Address> addAddress(Address address) {
    var message =
        new mainframe.client.messages.customer.AddAddress(
            address.city(),
            address.country(),
            address.customerNumber(),
            address.isMailing(),
            address.isPrimary(),
            address.line1(),
            address.line2(),
            address.otherType(),
            address.postCode(),
            address.suburb(),
            address.type());
    return send(
        message,
        () ->
            new Address(
                message.getId(),
                message.getCustomer(),
                message.getLine1(),
                message.getLine2(),
                message.getSuburb(),
                message.getCity(),
                message.getCountry(),
                message.getPostCode(),
                message.getType(),
                message.getOtherType(),
                message.getIsMailing(),
                message.getIsPrimary()));
  }

  @Override
  public OperationResult<Boolean> deleteAddress(String customerNumber, long addressId) {
    var message = new mainframe.client.messages.customer.DeleteAddress(customerNumber, addressId);
    return send(message, () -> true);
  }

  @Override
  public OperationResult<Address> updateAddress(Address address) {
    // id must not be null for an update, so make sure there is logic in the service to prevent this
    // from reaching here
    var message =
        new mainframe.client.messages.customer.UpdateAddress(
            address.city(),
            address.country(),
            address.customerNumber(),
            address.id(),
            address.isMailing(),
            address.isPrimary(),
            address.line1(),
            address.line2(),
            address.otherType(),
            address.postCode(),
            address.suburb(),
            address.type());
    return send(
        message,
        () ->
            new Address(
                message.getId(),
                message.getCustomer(),
                message.getLine1(),
                message.getLine2(),
                message.getSuburb(),
                message.getCity(),
                message.getCountry(),
                message.getPostCode(),
                message.getType(),
                message.getOtherType(),
                message.getIsMailing(),
                message.getIsPrimary()));
  }

  @Override
  public OperationResult<Page<AddressSummary>> listAddresses(
      String customerNumber, long page, long size) {
    var message = new mainframe.client.messages.customerAddress.List(customerNumber, page, size);

    return send(message, () -> {
      var addresses = new ArrayList<AddressSummary>();
      for (var item : message.getItems()) {
        addresses.add(
            new AddressSummary(
                item.getId(),
                item.getCity(),
                item.getCountry(),
                item.getType(),
                "y".equals(item.getIsMailing()),
                "y".equals(item.getIsPrimary())));
      }
      return new Page<>(addresses, message.getPage(), size, message.getTotal());
    });
  }

  @Override
  public OperationResult<Address> retrieveAddress(String customerNumber, long addressId) {
    var message = new mainframe.client.messages.customerAddress.Retrieve(addressId, customerNumber);

    return send(
        message,
        () ->
            new Address(
                message.getId(),
                message.getCustomerNumber(),
                message.getLine1(),
                message.getLine2(),
                message.getSuburb(),
                message.getCity(),
                message.getCountry(),
                message.getPostCode(),
                message.getType(),
                message.getOtherType(),
                message.getIsMailing(),
                message.getIsPrimary()));
  }

  // Phone related methods
  @Override
  public OperationResult<Phone> addPhone(Phone phone) {
    var message =
        new mainframe.client.messages.customer.AddPhoneNumber(
            phone.canTxt(),
            phone.countryCode(),
            phone.customerNumber(),
            phone.isPrimary(),
            phone.number(),
            phone.otherType(),
            phone.type());
    return send(
        message,
        () ->
            new Phone(
                message.getId(),
                message.getCanTxt(),
                message.getCountryCode(),
                null,
                message.getCustomer(),
                message.getIsPrimary(),
                message.getNumber(),
                message.getOtherType(),
                message.getType()));
  }

  @Override
  public OperationResult<Boolean> deletePhone(String customerNumber, long phoneId) {
    var message = new mainframe.client.messages.customer.DeletePhoneNumber(customerNumber, phoneId);
    return send(message, () -> true);
  }

  @Override
  public OperationResult<Phone> updatePhone(Phone phone) {
    var message =
        new mainframe.client.messages.customer.UpdatePhoneNumber(
            phone.canTxt(),
            phone.countryCode(),
            phone.customerNumber(),
            phone.id(),
            phone.isPrimary(),
            phone.number(),
            phone.otherType(),
            phone.type());
    return send(
        message,
        () ->
            new Phone(
                message.getId(),
                message.getCanTxt(),
                message.getCountryCode(),
                null,
                message.getCustomer(),
                message.getIsPrimary(),
                message.getNumber(),
                message.getOtherType(),
                message.getType()));
  }

  @Override
  public OperationResult<Page<PhoneSummary>> listPhones(
      String customerNumber, long page, long size) {
    var message =
        new mainframe.client.messages.customerPhoneNumber.List(customerNumber, page, size);
    return send(message, () -> {
      var phones = new ArrayList<PhoneSummary>();
      for (var item : message.getItems()) {
        phones.add(
            new PhoneSummary(
                item.getId(),
                item.getCountryCode(),
                item.getNumber(),
                item.getType(),
                "y".equals(item.getCanTxt()),
                "y".equals(item.getIsPrimary())));
      }
      return new Page<>(phones, message.getPage(), size, message.getTotal());
    });
  }

  @Override
  public OperationResult<Phone> retrievePhone(String customerNumber, long phoneId) {
    var message =
        new mainframe.client.messages.customerPhoneNumber.Retrieve(phoneId, customerNumber);
    return send(
        message,
        () ->
            new Phone(
                message.getId(),
                message.getCanTxt(),
                message.getCountryCode(),
                message.getCustomerName(),
                message.getCustomerNumber(),
                message.getIsPrimary(),
                message.getNumber(),
                message.getOtherType(),
                message.getType()));
  }

  // Email related methods
  @Override
  public OperationResult<Email> addEmail(Email email) {
    var message =
        new mainframe.client.messages.customer.AddEmail(
            email.address(),
            email.customerNumber(),
            email.isPrimary(),
            email.otherType(),
            email.type());
    return send(
        message,
        () ->
            new Email(
                message.getId(),
                null,
                message.getCustomer(),
                message.getAddress(),
                message.getIsPrimary(),
                message.getOtherType(),
                message.getType()));
  }

  @Override
  public OperationResult<Boolean> deleteEmail(String customerNumber, long emailId) {
    var message = new mainframe.client.messages.customer.DeleteEmail(customerNumber, emailId);
    return send(message, () -> true);
  }

  @Override
  public OperationResult<Email> updateEmail(Email email) {
    var message =
        new mainframe.client.messages.customer.UpdateEmail(
            email.address(),
            email.customerNumber(),
            email.id(),
            email.isPrimary(),
            email.otherType(),
            email.type());
    return send(
        message,
        () ->
            new Email(
                message.getId(),
                null,
                message.getCustomer(),
                message.getAddress(),
                message.getIsPrimary(),
                message.getOtherType(),
                message.getType()));
  }

  @Override
  public OperationResult<Page<EmailSummary>> listEmails(
      String customerNumber, long page, long size) {
    var message = new mainframe.client.messages.customerEmail.List(customerNumber, page, size);
    return send(message, () -> {
      var emails = new ArrayList<EmailSummary>();
      for (var item : message.getItems()) {
        emails.add(
            new EmailSummary(
                item.getId(), item.getAddress(), "y".equals(item.getIsPrimary()), item.getType()));
      }
      return new Page<>(emails, message.getPage(), size, message.getTotal());
    });
  }

  @Override
  public OperationResult<Email> retrieveEmail(String customerNumber, long emailId) {
    var message = new mainframe.client.messages.customerEmail.Retrieve(emailId, customerNumber);
    return send(
        message,
        () ->
            new Email(
                message.getId(),
                message.getCustomerName(),
                message.getCustomerNumber(),
                message.getAddress(),
                message.getIsPrimary(),
                message.getOtherType(),
                message.getType()));
  }

  // Employer related methods

  @Override
  public OperationResult<Employer> addEmployer(Employer employer) {
    var message =
        new mainframe.client.messages.customer.AddEmployer(
            employer.customerNumber(),
            employer.name(),
            employer.position(),
            employer.salary(),
            employer.type());
    return send(
        message,
        () ->
            new Employer(
                message.getId(),
                null,
                message.getCustomer(),
                message.getName(),
                message.getPosition(),
                message.getSalary(),
                message.getType()));
  }

  @Override
  public OperationResult<Boolean> deleteEmployer(String customerNumber, long employerId) {
    var message = new mainframe.client.messages.customer.DeleteEmployer(customerNumber, employerId);
    return send(message, () -> true);
  }

  @Override
  public OperationResult<Employer> updateEmployer(Employer employer) {
    var message =
        new mainframe.client.messages.customer.UpdateEmployer(
            employer.customerNumber(),
            employer.id(),
            employer.name(),
            employer.position(),
            employer.salary(),
            employer.type());
    return send(
        message,
        () ->
            new Employer(
                message.getId(),
                null,
                message.getCustomer(),
                message.getName(),
                message.getPosition(),
                message.getSalary(),
                message.getType()));
  }

  @Override
  public OperationResult<Page<EmployerSummary>> listEmployers(
      String customerNumber, long page, long size) {
    var message = new mainframe.client.messages.customerEmployer.List(customerNumber, page, size);
    return send(message, () -> {
      var employers = new ArrayList<EmployerSummary>();
      for (var item : message.getItems()) {
        employers.add(new EmployerSummary(item.getId(), item.getName(), item.getType()));
      }
      return new Page<>(employers, message.getPage(), size, message.getTotal());
    });
  }

  @Override
  public OperationResult<Employer> retrieveEmployer(String customerNumber, long employerId) {
    var message =
        new mainframe.client.messages.customerEmployer.Retrieve(employerId, customerNumber);
    return send(
        message,
        () ->
            new Employer(
                message.getId(),
                message.getCustomerName(),
                message.getCustomerNumber(),
                message.getName(),
                message.getPosition(),
                message.getSalary(),
                message.getType()));
  }

  // Customer document metadata. The mainframe does not provide an update message.
  @Override
  public OperationResult<CustomerDocument> addDocument(
      String customerNumber, DocumentMetadata metadata) {
    var message = new mainframe.client.messages.customer.AddDocument(
        customerNumber, metadata.fileName(), metadata.fileType(), metadata.size(), metadata.type());
    return send(message, () -> new CustomerDocument(
        message.getId(), message.getCustomer(), message.getFileName(),
        message.getFileType(), message.getSize(), message.getType()));
  }

  @Override
  public OperationResult<Boolean> deleteDocument(String customerNumber, long documentId) {
    var message = new mainframe.client.messages.customer.DeleteDocument(customerNumber, documentId);
    return send(message, () -> true);
  }

  @Override
  public OperationResult<Page<CustomerDocumentSummary>> listDocuments(
      String customerNumber, long page, long size) {
    var message = new mainframe.client.messages.customerDocument.List(customerNumber, page, size);
    return send(message, () -> {
      var documents = new ArrayList<CustomerDocumentSummary>();
      for (var item : message.getItems()) {
        documents.add(new CustomerDocumentSummary(item.getId(), item.getFileName(), item.getType()));
      }
      return new Page<>(documents, message.getPage(), size, message.getTotal());
    });
  }

  @Override
  public OperationResult<CustomerDocument> retrieveDocument(String customerNumber, long documentId) {
    var message = new mainframe.client.messages.customerDocument.Retrieve(documentId, customerNumber);
    return send(message, () -> new CustomerDocument(
        message.getId(), message.getCustomerNumber(), message.getFileName(),
        message.getFileType(), message.getSize(), message.getType()));
  }
}
