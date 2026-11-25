package ua.edu.onu.java.sem1.t07_solid;

/**
 * Абстракція сповіщень (DIP).
 */
public interface Notifier {
    void send(String recipient, String message);
}
