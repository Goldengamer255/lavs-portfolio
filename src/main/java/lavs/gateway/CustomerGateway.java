package lavs.gateway;

import lavs.domain.Address;
import lavs.domain.AddressSummary;
import lavs.domain.Customer;
import lavs.domain.CustomerSummary;
import lavs.domain.NewCustomer;
import lavs.domain.Page;

public interface CustomerGateway {
    OperationResult<Customer> create(NewCustomer input);

    OperationResult<Customer> retrieve(String customerNumber);

    OperationResult<Page<CustomerSummary>> search(String nameQuery, long page, long size);

    OperationResult<Address> addAddress(Address address);
    
    OperationResult<Boolean> deleteAddress(String customerNumber, long addressId);

    OperationResult<Customer> update(Customer customer);

    OperationResult<String> changeStatus(String customerNumber, String status);

    OperationResult<Address> updateAddress(Address address);

    OperationResult<Page<AddressSummary>> listAddresses(String customerNumber, long page, long size);

    OperationResult<Address> retrieveAddress(String customerNumber, long addressId);
}
