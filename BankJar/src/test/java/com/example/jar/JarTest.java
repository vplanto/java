package com.example.jar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JarTest {

    @Test
    void testAddSingleDonation() {
        Jar jar = new Jar(1000);
        jar.addDonation("Vitalii", 100);

        assertEquals(100, jar.getTotalAmount());
        assertEquals(1, jar.getUniqueDonatorsCount());
    }

    @Test
    void testAddMultipleDonationsFromSamePerson() {
        Jar jar = new Jar(1000);
        jar.addDonation("Olena", 50);
        jar.addDonation("Olena", 150);

        assertEquals(200, jar.getTotalAmount());
        assertEquals(1, jar.getUniqueDonatorsCount());
    }

    @Test
    void testUniqueDonatorsCount() {
        Jar jar = new Jar(1000);
        jar.addDonation("Andrii", 100);
        jar.addDonation("Bohdan", 200);
        jar.addDonation("Andrii", 50);

        assertEquals(350, jar.getTotalAmount());
        assertEquals(2, jar.getUniqueDonatorsCount());
    }
}