package ua.edu.onu.java.sem1.t06_exceptions;

/**
 * Checked-виняток для ситуації нестачі коштів на рахунку.
 */
public class InsufficientFundsException extends Exception {

    private final long requested;
    private final long available;

    public InsufficientFundsException(long requested, long available) {
        super("Недостатньо коштів: запитано " + requested + ", доступно " + available);
        this.requested = requested;
        this.available = available;
    }

    public long getRequested() {
        return requested;
    }

    public long getAvailable() {
        return available;
    }
}
