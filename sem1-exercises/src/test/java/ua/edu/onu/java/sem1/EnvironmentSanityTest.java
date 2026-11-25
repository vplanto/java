package ua.edu.onu.java.sem1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentSanityTest {

    @Test
    void javaVersionShouldBeAtLeast21() {
        int featureVersion = Runtime.version().feature();
        assertTrue(featureVersion >= 21, "Потрібна JDK 21 LTS або новіша, поточна: " + featureVersion);
    }
}
