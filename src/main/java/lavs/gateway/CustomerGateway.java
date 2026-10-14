package lavs.gateway;

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

public interface CustomerGateway {
    //Customer related methods
    OperationResult<Customer> create(NewCustomer input);

    OperationResult<Customer> retrieve(String customerNumber);

    OperationResult<Page<CustomerSummary>> search(String nameQuery, long page, long size);

    OperationResult<Customer> update(Customer customer);

    OperationResult<String> changeStatus(String customerNumber, String status);

    //Address related methods
    OperationResult<Address> addAddress(Address address);
    
    OperationResult<Boolean> deleteAddress(String customerNumber, long addressId);

    OperationResult<Address> updateAddress(Address address);

    OperationResult<Page<AddressSummary>> listAddresses(String customerNumber, long page, long size);

    OperationResult<Address> retrieveAddress(String customerNumber, long addressId);

    //Phone related methods
    OperationResult<Phone> addPhone(Phone phone);
    
    OperationResult<Boolean> deletePhone(String customerNumber, long phoneId);

    OperationResult<Phone> updatePhone(Phone phone);

    OperationResult<Page<PhoneSummary>> listPhones(String customerNumber, long page, long size);

    OperationResult<Phone> retrievePhone(String customerNumber, long phoneId);

    //Email related methods
    OperationResult<Email> addEmail(Email email);

    OperationResult<Boolean> deleteEmail(String customerNumber, long emailId);

    OperationResult<Email> updateEmail(Email email);

    OperationResult<Page<EmailSummary>> listEmails(String customerNumber, long page, long size);

    OperationResult<Email> retrieveEmail(String customerNumber, long emailId);

    //Employer related methods

    OperationResult<Employer> addEmployer(Employer employer);

    OperationResult<Boolean> deleteEmployer(String customerNumber, long employerId);

    OperationResult<Employer> updateEmployer(Employer employer);

    OperationResult<Page<EmployerSummary>> listEmployers(String customerNumber, long page, long size);

    OperationResult<Employer> retrieveEmployer(String customerNumber, long employerId);
}                                                                                      