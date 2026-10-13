package lavs.gateway;

import lavs.domain.Address;
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
}
