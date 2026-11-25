package ua.edu.onu.java.sem1.t02_oop;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeviceContractTest {

    @Test
    void sensorHealthAndStatusWorkCorrectly() {
        TemperatureSensor ok = new TemperatureSensor("SN-001", "TempX", -10.0, 50.0, 22.5);
        assertTrue(ok.isHealthy());
        assertEquals("SN-001: OK", ok.statusSummary());

        ok.updateReading(60.0);
        assertFalse(ok.isHealthy());
        assertEquals("SN-001: ALERT", ok.statusSummary());
    }

    @Test
    void equalsAndHashCodeFollowSerialNumberContract() {
        Device d1 = new TemperatureSensor("SN-100", "ModelA", 0, 100, 20);
        Device d2 = new TemperatureSensor("SN-100", "ModelB", -50, 50, 10);
        Device d3 = new TemperatureSensor("SN-200", "ModelA", 0, 100, 20);

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertNotEquals(d1, d3);

        Set<Device> set = new HashSet<>();
        set.add(d1);
        set.add(d2);
        set.add(d3);
        assertEquals(2, set.size());
    }

    @Test
    void comparableSortsBySerialNumber() {
        List<Device> devices = new ArrayList<>(List.of(
                new TemperatureSensor("SN-03", "M", 0, 10, 5),
                new TemperatureSensor("SN-01", "M", 0, 10, 5),
                new TemperatureSensor("SN-02", "M", 0, 10, 5)
        ));
        Collections.sort(devices);
        assertEquals(List.of("SN-01", "SN-02", "SN-03"),
                devices.stream().map(Device::getSerialNumber).toList());
    }
}
