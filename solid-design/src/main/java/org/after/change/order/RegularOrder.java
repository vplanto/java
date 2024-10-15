package org.after.change.order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.after.change.product.ItemPrice;

public class RegularOrder implements Order {
    private final Map<ItemPrice, Long> items;

    public RegularOrder() {
        items = new HashMap<>();
    }

    @Override
    public void addItem(ItemPrice item, long quantity) {
        items.put(item, quantity);
    }

    @Override
    public void removeItem(ItemPrice item) {
        items.remove(item);
    }

    @Override
    public long getQuantity(ItemPrice item) {
        return items.getOrDefault(item, 0L);
    }

    @Override
    public List<ItemPrice> getAllItems() {
        return new ArrayList<>(items.keySet());
    }

    @Override
    public String toString() {
        return "RegularOrder{" +
                "items=" + items +
                '}';
    }
}
