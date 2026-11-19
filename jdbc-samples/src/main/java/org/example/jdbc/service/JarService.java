package org.example.jdbc.service;

import java.util.List;
import org.example.jdbc.model.Donation;
import org.example.jdbc.repository.DonationRepository;

/**
 * Високорівневий сервіс "Банки": знає бізнес-правила, але не знає деталей БД (DIP).
 */
public class JarService {

    private final DonationRepository repository;

    public JarService(DonationRepository repository) {
        this.repository = repository;
    }

    public void donate(String donorName, long amountKopiyky) {
        if (donorName == null || donorName.isBlank()) {
            throw new IllegalArgumentException("Ім'я донатора не може бути порожнім");
        }
        if (amountKopiyky <= 0) {
            throw new IllegalArgumentException("Сума донату має бути додатною");
        }
        repository.save(donorName.trim(), amountKopiyky);
    }

    public List<Donation> history() {
        return repository.findAll();
    }

    public List<Donation> historyByDonor(String donorName) {
        return repository.findByDonor(donorName);
    }

    public long totalKopiyky() {
        return repository.getTotalAmount();
    }
}
