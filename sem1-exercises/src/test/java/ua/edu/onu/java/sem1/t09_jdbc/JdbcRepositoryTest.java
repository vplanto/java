package ua.edu.onu.java.sem1.t09_jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcRepositoryTest {

    @Test
    void saveFindAndPreventSqlInjection() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:books_test;DB_CLOSE_DELAY=-1")) {
            BookRepository repo = new BookRepository(conn);
            repo.initSchema();

            long id1 = repo.save("Effective Java", "Joshua Bloch");
            long id2 = repo.save("Clean Architecture", "Robert Martin");
            assertTrue(id1 > 0);
            assertTrue(id2 > id1);

            List<BookRepository.Book> blochBooks = repo.findByAuthor("Joshua Bloch");
            assertEquals(1, blochBooks.size());
            assertEquals("Effective Java", blochBooks.get(0).title());

            // Спроба SQL-ін'єкції має повернути порожній список, а не всі записи
            List<BookRepository.Book> injected = repo.findByAuthor("' OR '1'='1");
            assertTrue(injected.isEmpty());
        }
    }
}
