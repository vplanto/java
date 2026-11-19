package org.example.jdbc.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.example.jdbc.model.Donation;

/**
 * Початковий стан історії: сховище лише в оперативній пам'яті.
 * Дані зникають одразу після завершення програми.
 */
public class InMemoryDonationRepository implements DonationRepository {

    private final List<Donation> storage = new ArrayList<>();
    private long sequence = 1L;

    @Override
    public void save(String donorName, long amountKopiyky) {
        storage.add(new Donation(sequence++, donorName, amountKopiyky, Instant.now()));
    }

    @Override
    public List<Donation> findAll() {
        return List.copyOf(storage);
    }

    @Override
    public List<Donation> findByDonor(String donorName) {
        return storage.stream()
                .filter(d -> d.donorName().equals(donorName))
                .toList();
    }

    @Override
    public long getTotalAmount() {
        return storage.stream()
                .mapToLong(Donation::amountKopiyky)
                .sum();
    }
}
