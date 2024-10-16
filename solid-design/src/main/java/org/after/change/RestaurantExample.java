package org.after.change;

import org.after.change.billing.RegularBill;
import org.after.change.billing.RegularTaxBill;
import org.after.change.customer.Customer;
import org.after.change.order.RegularOrder;
import org.after.change.product.RegularItem;
import org.after.change.report.JSONReport;

public class RestaurantExample {
    private final Customer customer;
    private final RegularOrder order;
    private final long gst;

    {
        this.order = new RegularOrder();
        order.addItem(new RegularItem("Alu Biryani", 185), 2);
        order.addItem(new RegularItem("Paneer Biryani", 200), 1);
        order.addItem(new RegularItem("Chicken Biryani", 250), 2);

        this.customer = new Customer("1", "Naveen", "25");
        this.gst = 18;
    }

    public void demoRun() {
        double total = new RegularBill().calculateTotal(this.order);

        System.out.println(customer);
        System.out.println(order);
        System.out.println("Total = " + total);
        total = new RegularTaxBill(this.gst).calculateTotal(this.order);
        System.out.println("Total with taxes = " + total);
        new JSONReport().generate(customer);
    }

    public static void main(String[] args) {
        new RestaurantExample().demoRun();
    }
}
