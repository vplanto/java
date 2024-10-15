package org.before.change;

import java.util.ArrayList;
import java.util.List;

public class ResturantExample {

    private Customer customer;
    private long gst;

    {

        Item item1 = new Item("Alu Biriyani", 2, 185);
        Item item2 = new Item("Paneer Biriyani", 1, 200);
        Item item3 = new Item("Chicken Biriyani", 2, 250);

        List<Item> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        items.add(item3);

        this.customer = new Customer("1", "Naveen", "25");
        this.gst = 18;

        customer.setItems(items);
    }

    public void demoRun() {
        ResturantExample example = new ResturantExample();
        double bill = example.calculateBill(customer, gst);
        customer.setBillAmount(bill);
        example.generateReportCSV(customer);
        System.out.println(customer);
    }

    public double calculateBill(Customer customer, long gst) {
        double bill = 0;
        List<Item> items = customer.getItems();
        for (Item item : items) {
            bill += (item.getPrice()*item.getQuantity());
        }

        bill =bill+(bill * gst) / 100;
        return bill;
    }

    public void generateReportCSV(Customer customer) {
        System.out.println("CSV REPORT");

    }

    public void generateReportPDF(Customer customer) {
        System.out.println("PDF REPORT");

    }
    public void generateReportJSON(Customer customer) {
        System.out.println("JSON REPORT");

    }
    public void generateReportXML(Customer customer) {
        System.out.println("XML REPORT");

    }


}