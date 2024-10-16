package org.after.change.report;

import org.after.change.customer.Customer;

public class JSONReport implements Report{
    @Override
    public void generate(Customer customer) {
        System.out.println("JSON REPORT");
    }
}
