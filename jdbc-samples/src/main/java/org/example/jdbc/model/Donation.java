package org.example.jdbc.model;

import java.time.Instant;

/**
 * Незмінна модель донату в "Банку".
 */
public record Donation(long id, String donorName, long amountKopiyky, Instant createdAt) {
}
