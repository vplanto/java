package com.example.jar;

import java.util.*;

public class Jar {
    private final double targetAmount; // Цільова сума збору
    private final List<Donation> donations; // Для зберігання ВСІХ донатів по порядку
    private final Map<String, Double> donatorTotals; // Для підрахунку загальної суми від кожної людини
    private final Set<String> uniqueDonators; // Для зберігання унікальних імен донатерів

    public Jar(double targetAmount) {
        this.targetAmount = targetAmount;
        this.donations = new ArrayList<>();
        this.donatorTotals = new HashMap<>();
        this.uniqueDonators = new HashSet<>();
    }

    public void addDonation(String name, double amount) {
        // 1. Створюємо об'єкт донату
        Donation newDonation = new Donation(name, amount);

        // 2. Додаємо його до загальної історії (List)
        donations.add(newDonation);

        // 3. Додаємо ім'я до множини унікальних донатерів (Set)
        // Якщо таке ім'я вже є, нічого не зміниться
        uniqueDonators.add(name);

        // 4. Оновлюємо загальну суму для цієї людини (Map)
        double currentTotal = donatorTotals.getOrDefault(name, 0.0);
        donatorTotals.put(name, currentTotal + amount);
    }

    public double getTotalAmount() {
        double total = 0;
        for (Donation donation : donations) {
            total += donation.amount();
        }
        return total;
    }

    public int getUniqueDonatorsCount() {
        return uniqueDonators.size();
    }

    public double getTargetAmount() {
        return this.targetAmount;
    }

    public List<Donation> getDonationHistory() {
        return new ArrayList<>(donations); // Повертаємо копію, щоб захистити оригінальний список
    }

}