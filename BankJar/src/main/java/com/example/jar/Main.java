package com.example.jar;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Jar myJar = new Jar(10000); // Створюємо банку на 10 000

        // Імітуємо донати
        myJar.addDonation("Степан", 500);
        myJar.addDonation("Марія", 1000);
        myJar.addDonation("Степан", 250);
        myJar.addDonation("Ігор", 2000);

        // Виводимо статистику
        System.out.println("=== ЗВІТ ПО БАНЦІ ===");
        System.out.printf("Ціль: %.2f грн%n", myJar.getTargetAmount());
        System.out.printf("Зібрано: %.2f грн%n", myJar.getTotalAmount());
        System.out.println("Кількість унікальних донатерів: " + myJar.getUniqueDonatorsCount());
        System.out.println("\n--- Історія донатів ---");
        for (Donation donation : myJar.getDonationHistory()) {
            System.out.printf("%s задонатив(ла) %.2f грн%n", donation.donatorName(), donation.amount());
        }
    }
}
