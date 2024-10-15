package org.after.change.billing;

import org.after.change.order.Order;
import org.after.change.product.ItemPrice;

import java.util.List;

public class RegularBill implements Bill{
    @Override
    public double calculateTotal(Order order) {
        long total = 0;
        List<ItemPrice> items = order.getAllItems();

        for (ItemPrice item : items) {
            long quantity = order.getQuantity(item);
            total += item.getPrice() * quantity;
        }

        return total;
    }
}