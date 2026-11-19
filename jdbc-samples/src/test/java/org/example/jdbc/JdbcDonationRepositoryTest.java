package org.example.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.example.jdbc.model.Donation;
import org.example.jdbc.repository.DonationRepository;
import org.example.jdbc.repository.InMemoryDonationRepository;
import org.example.jdbc.repository.JdbcDonationRepository;
import org.example.jdbc.service.JarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcDonationRepositoryTest {

    private JdbcDonationRepository repository;
    private JarService jarService;

    @BeforeEach
    void setUp() {
        String uniqueDbUrl = "jdbc:h2:mem:jar_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        repository = new JdbcDonationRepository(uniqueDbUrl, "sa", "");
        jarService = new JarService(repository);
    }

    @Test
    void savesAndReadsDonationsViaJdbc() {
        jarService.donate("Олена", 15_000);
        jarService.donate("Максим", 25_000);

        List<Donation> history = jarService.history();
        assertEquals(2, history.size());
        assertEquals("Олена", history.get(0).donorName());
        assertEquals(15_000, history.get(0).amountKopiyky());
        assertEquals(40_000, jarService.totalKopiyky());
    }

    @Test
    void preparedStatementBlocksSqlInjection() {
        jarService.donate("Олена", 10_000);
        jarService.donate("Андрій", 20_000);

        String injectionPayload = "' OR '1'='1";

        List<Donation> unsafeResult = repository.findByDonorUnsafe(injectionPayload);
        List<Donation> safeResult = jarService.historyByDonor(injectionPayload);

        assertEquals(2, unsafeResult.size());
        assertTrue(safeResult.isEmpty());
    }

    @Test
    void jarServiceWorksWithAnyRepositoryImplementation() {
        DonationRepository inMemory = new InMemoryDonationRepository();
        JarService memoryService = new JarService(inMemory);

        memoryService.donate("Ірина", 5_000);
        jarService.donate("Ірина", 5_000);

        assertEquals(memoryService.totalKopiyky(), jarService.totalKopiyky());
        assertEquals(1, memoryService.historyByDonor("Ірина").size());
        assertEquals(1, jarService.historyByDonor("Ірина").size());
    }

    @Test
    void rejectsInvalidDonationsBeforeTouchingDatabase() {
        assertThrows(IllegalArgumentException.class, () -> jarService.donate(" ", 1_000));
        assertThrows(IllegalArgumentException.class, () -> jarService.donate("Олег", 0));
        assertEquals(0L, jarService.totalKopiyky());
    }
}
