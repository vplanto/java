package ua.edu.onu.java.sem1.t02_oop;

import java.util.Objects;

/**
 * T02: Базовий клас пристрою з контрактом Object та Comparable.
 * Два пристрої вважаються рівними, якщо збігається їхній serialNumber.
 * Сортування за замовчуванням - лексикографічно за serialNumber.
 */
public abstract class Device implements Diagnosable, Comparable<Device> {

    private final String serialNumber;
    private final String model;

    protected Device(String serialNumber, String model) {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new IllegalArgumentException("serialNumber не може бути порожнім");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model не може бути порожнім");
        }
        this.serialNumber = serialNumber;
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getModel() {
        return model;
    }

    @Override
    public int compareTo(Device other) {
        throw new UnsupportedOperationException("TODO: порівняння за serialNumber");
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("TODO: рівність за serialNumber");
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("TODO: hashCode узгоджений з equals");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{serialNumber='" + serialNumber + "', model='" + model + "'}";
    }
}
