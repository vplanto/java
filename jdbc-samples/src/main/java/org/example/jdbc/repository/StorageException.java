package org.example.jdbc.repository;

/**
 * Обгортка над низькорівневими помилками збереження (зокрема SQLException),
 * щоб не прив'язувати контракт інтерфейсу до деталей JDBC.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
