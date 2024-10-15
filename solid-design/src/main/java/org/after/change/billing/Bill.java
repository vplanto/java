package org.after.change.billing;

import org.after.change.order.Order;

public interface Bill {
    double calculateTotal(Order order);
}
