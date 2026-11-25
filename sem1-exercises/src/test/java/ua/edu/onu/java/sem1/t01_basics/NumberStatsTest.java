package ua.edu.onu.java.sem1.t01_basics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumberStatsTest {

    @Test
    void sumHandlesNormalEmptyAndOverflow() {
        assertEquals(0L, NumberStats.sum(new int[]{}));
        assertEquals(15L, NumberStats.sum(new int[]{1, 2, 3, 4, 5}));
        assertEquals(2L * Integer.MAX_VALUE, NumberStats.sum(new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE}));
        assertThrows(IllegalArgumentException.class, () -> NumberStats.sum(null));
    }

    @Test
    void maxHandlesNegativesAndInvalidInput() {
        assertEquals(-2, NumberStats.max(new int[]{-10, -2, -50}));
        assertEquals(42, NumberStats.max(new int[]{42}));
        assertThrows(IllegalArgumentException.class, () -> NumberStats.max(new int[]{}));
        assertThrows(IllegalArgumentException.class, () -> NumberStats.max(null));
    }

    @Test
    void countEvenCountsZeroAndNegatives() {
        assertEquals(3, NumberStats.countEven(new int[]{-4, -3, 0, 1, 2}));
        assertEquals(0, NumberStats.countEven(new int[]{1, 3, 5}));
        assertThrows(IllegalArgumentException.class, () -> NumberStats.countEven(null));
    }

    @Test
    void isPrimeChecksEdgeCases() {
        assertFalse(NumberStats.isPrime(-5));
        assertFalse(NumberStats.isPrime(0));
        assertFalse(NumberStats.isPrime(1));
        assertTrue(NumberStats.isPrime(2));
        assertTrue(NumberStats.isPrime(29));
        assertFalse(NumberStats.isPrime(100));
    }
}
