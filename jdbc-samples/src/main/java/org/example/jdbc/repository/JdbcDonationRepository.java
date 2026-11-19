package org.example.jdbc.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.example.jdbc.model.Donation;

/**
 * Реалізація сховища на чистому JDBC (5-кроковий життєвий цикл з Лекції 9).
 * Використовує try-with-resources для автоматичного закриття Connection, Statement і ResultSet.
 */
public class JdbcDonationRepository implements DonationRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS donations (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                donor_name VARCHAR(100) NOT NULL,
                amount_kopiyky BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL
            )
            """;

    private final String jdbcUrl;
    private final String user;
    private final String password;

    public JdbcDonationRepository(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        initSchema();
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    private void initSchema() {
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        } catch (SQLException e) {
            throw new StorageException("Не вдалося ініціалізувати таблицю donations", e);
        }
    }

    @Override
    public void save(String donorName, long amountKopiyky) {
        String sql = "INSERT INTO donations (donor_name, amount_kopiyky, created_at) VALUES (?, ?, ?)";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donorName);
            ps.setLong(2, amountKopiyky);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Не вдалося зберегти донат", e);
        }
    }

    @Override
    public List<Donation> findAll() {
        String sql = "SELECT id, donor_name, amount_kopiyky, created_at FROM donations ORDER BY id";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapRows(rs);
        } catch (SQLException e) {
            throw new StorageException("Не вдалося прочитати список донатів", e);
        }
    }

    @Override
    public List<Donation> findByDonor(String donorName) {
        String sql = "SELECT id, donor_name, amount_kopiyky, created_at FROM donations WHERE donor_name = ? ORDER BY id";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donorName);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        } catch (SQLException e) {
            throw new StorageException("Не вдалося знайти донати за ім'ям донатора", e);
        }
    }

    /**
     * АНТИПРИКЛАД: конкатенація введення користувача в SQL робить запит вразливим до SQL-ін'єкції.
     * Залишено виключно для демонстрації різниці між Statement і PreparedStatement.
     */
    public List<Donation> findByDonorUnsafe(String donorName) {
        String sql = "SELECT id, donor_name, amount_kopiyky, created_at FROM donations WHERE donor_name = '"
                + donorName + "' ORDER BY id";
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapRows(rs);
        } catch (SQLException e) {
            throw new StorageException("Помилка виконання небезпечного запиту", e);
        }
    }

    @Override
    public long getTotalAmount() {
        String sql = "SELECT COALESCE(SUM(amount_kopiyky), 0) AS total FROM donations";
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong("total") : 0L;
        } catch (SQLException e) {
            throw new StorageException("Не вдалося порахувати загальну суму", e);
        }
    }

    private static List<Donation> mapRows(ResultSet rs) throws SQLException {
        List<Donation> result = new ArrayList<>();
        while (rs.next()) {
            result.add(new Donation(
                    rs.getLong("id"),
                    rs.getString("donor_name"),
                    rs.getLong("amount_kopiyky"),
                    rs.getTimestamp("created_at").toInstant()
            ));
        }
        return result;
    }
}
