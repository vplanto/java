package org.after.change.report;

import org.after.change.customer.Customer;

public interface Report {
    void generate(Customer customer);
}