package org.example.jdbc.repository;

import java.util.List;
import org.example.jdbc.model.Donation;

/**
 * Абстракція сховища донатів (DIP з Лекції 5 та Лекції 9).
 * Бізнес-логіка залежить від цього контракту, а не від SQL чи конкретної СУБД.
 */
public interface DonationRepository {

    void save(String donorName, long amountKopiyky);

    List<Donation> findAll();

    List<Donation> findByDonor(String donorName);

    long getTotalAmount();
}
