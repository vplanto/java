package ua.edu.onu.java.sem1.t02_oop;

/**
 * Контракт діагностики пристрою.
 */
public interface Diagnosable {
    boolean isHealthy();

    String statusSummary();
}
