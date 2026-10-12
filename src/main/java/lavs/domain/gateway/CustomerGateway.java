package lavs.domain.gateway;

import lavs.domain.Customer;
import lavs.domain.NewCustomer;
import lavs.domain.Address;
import java.util.List;

public interface CustomerGateway {
    OperationResult<Customer> create(NewCustomer input);

    OperationResult<Customer> retrieve(String customerNumber);

    OperationResult<List<Customer>> search(String nameQuery);

    OperationResult<Address> addAddress(Address address);
    OperationResult<Boolean> deleteAddress(Long addressId);
}
