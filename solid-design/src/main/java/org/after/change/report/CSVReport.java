package org.after.change.report;

import org.after.change.customer.Customer;

public class CSVReport implements Report {
    @Override
    public void generate(Customer customer) {
        System.out.println("CSV REPORT");
    }
}
