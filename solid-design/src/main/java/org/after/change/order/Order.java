package org.after.change.order;

import org.after.change.product.ItemPrice;

import java.util.List;

public interface Order {
    void addItem(ItemPrice item, long quantity);

    void removeItem(ItemPrice item);

    long getQuantity(ItemPrice item);

    List<ItemPrice> getAllItems();
}
