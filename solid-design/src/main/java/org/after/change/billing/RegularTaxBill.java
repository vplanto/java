package org.after.change.billing;

import org.after.change.order.Order;

public class RegularTaxBill implements Bill {
    private final long gst;

    public RegularTaxBill(long gst) {
        this.gst = gst;
    }

    @Override
    public double calculateTotal(Order order) {
        return new RegularBill().calculateTotal(order) * (1 + (double) gst / 100);
    }
}
