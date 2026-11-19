package org.example.jdbc;

import java.util.List;
import org.example.jdbc.model.Donation;
import org.example.jdbc.repository.InMemoryDonationRepository;
import org.example.jdbc.repository.JdbcDonationRepository;
import org.example.jdbc.service.JarService;

/**
 * Історія: перехід "Банки" від пам'яті до реляційної БД через JDBC.
 * При повторному запуску дані у ./tmp/jar_db зберігаються на диску.
 */
public class Main {

    public static void main(String[] args) {
        String jdbcUrl = "jdbc:h2:file:./tmp/jar_db";
        JdbcDonationRepository jdbcRepo = new JdbcDonationRepository(jdbcUrl, "sa", "");
        JarService jarService = new JarService(jdbcRepo);

        if (jarService.history().isEmpty()) {
            jarService.donate("Олена", 15_000);
            jarService.donate("Андрій", 50_000);
        } else {
            jarService.donate("Повторний запуск", 10_000);
        }

        System.out.println("=== 1. Історія донатів з БД (переживає перезапуск) ===");
        for (Donation donation : jarService.history()) {
            System.out.printf("#%d | %-18s | %d коп. | %s%n",
                    donation.id(), donation.donorName(), donation.amountKopiyky(), donation.createdAt());
        }
        System.out.println("Загалом у Банці: " + jarService.totalKopiyky() + " коп.");

        System.out.println("\n=== 2. SQL-ін'єкція: Statement vs PreparedStatement ===");
        String payload = "' OR '1'='1";
        List<Donation> leaked = jdbcRepo.findByDonorUnsafe(payload);
        List<Donation> safe = jarService.historyByDonor(payload);
        System.out.println("Unsafe Statement повернув записів: " + leaked.size());
        System.out.println("Safe PreparedStatement повернув записів: " + safe.size());

        System.out.println("\n=== 3. DIP у дії: підміна реалізації на InMemory ===");
        JarService memoryJar = new JarService(new InMemoryDonationRepository());
        memoryJar.donate("Тест у пам'яті", 2_500);
        System.out.println("InMemory баланс: " + memoryJar.totalKopiyky() + " коп.");
    }
}
