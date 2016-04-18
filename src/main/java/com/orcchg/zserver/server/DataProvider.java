package com.orcchg.zserver.server;

import com.orcchg.zserver.model.Address;
import com.orcchg.zserver.model.Customer;
import rx.Observable;

public interface DataProvider {

    Observable<Customer> getCustomers(int limit, int offset);
    Observable<Address> getAddress(int addressId);
}
