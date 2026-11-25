package ua.edu.onu.java.sem1.t09_jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * T09: JDBC CRUD-репозиторій на H2 із захистом від SQL-ін'єкцій (PreparedStatement).
 */
public class BookRepository {

    public record Book(long id, String title, String author) {
    }

    private final Connection connection;

    public BookRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Створює таблицю books (id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255), author VARCHAR(255)),
     * якщо вона ще не існує.
     */
    public void initSchema() throws SQLException {
        throw new UnsupportedOperationException("TODO: створити таблицю books");
    }

    /**
     * Додає книгу через PreparedStatement і повертає згенерований id.
     */
    public long save(String title, String author) throws SQLException {
        throw new UnsupportedOperationException("TODO: INSERT через PreparedStatement із RETURN_GENERATED_KEYS");
    }

    /**
     * Повертає всі книги вказаного автора (через PreparedStatement, стійкий до SQL-ін'єкцій).
     */
    public List<Book> findByAuthor(String author) throws SQLException {
        throw new UnsupportedOperationException("TODO: SELECT через PreparedStatement");
    }
}
