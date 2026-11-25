package ua.edu.onu.java.sem1.t02_oop;

/**
 * Датчик температури: вважається справним, якщо lastReading в межах [minTemp, maxTemp].
 */
public class TemperatureSensor extends Device {

    private final double minTemp;
    private final double maxTemp;
    private double lastReading;

    public TemperatureSensor(String serialNumber, String model, double minTemp, double maxTemp, double lastReading) {
        super(serialNumber, model);
        if (minTemp > maxTemp) {
            throw new IllegalArgumentException("minTemp має бути <= maxTemp");
        }
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.lastReading = lastReading;
    }

    public void updateReading(double value) {
        this.lastReading = value;
    }

    public double getLastReading() {
        return lastReading;
    }

    @Override
    public boolean isHealthy() {
        throw new UnsupportedOperationException("TODO: перевірити діапазон [minTemp, maxTemp]");
    }

    @Override
    public String statusSummary() {
        throw new UnsupportedOperationException("TODO: повернути рядок формату '<serial>: OK' або '<serial>: ALERT'");
    }
}
