package org.after.change.product;

public class RegularItem implements ItemPrice {
    private String name;
    private long price;

    public RegularItem(String name, long price) {
        this.name = name;
        this.price = price;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getPrice() {
        return price;
    }

    @Override
    public void setPrice(long price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Item [name=" + name + ", price=" + price + "]";
    }
}