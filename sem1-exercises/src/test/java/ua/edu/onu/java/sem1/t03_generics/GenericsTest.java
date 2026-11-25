package ua.edu.onu.java.sem1.t03_generics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class GenericsTest {

    @Test
    void resultBoxOkAndFailContracts() {
        ResultBox<Integer> ok = ResultBox.ok(42);
        assertTrue(ok.isSuccess());
        assertEquals(42, ok.getValue());
        assertNull(ok.getError());
        assertEquals(42, ok.orElse(0));

        ResultBox<String> fail = ResultBox.fail("timeout");
        assertFalse(fail.isSuccess());
        assertEquals("timeout", fail.getError());
        assertEquals("default", fail.orElse("default"));
        assertThrows(NoSuchElementException.class, fail::getValue);

        assertThrows(IllegalArgumentException.class, () -> ResultBox.ok(null));
        assertThrows(IllegalArgumentException.class, () -> ResultBox.fail("  "));
    }

    @Test
    void minMaxFinderWorksWithNumbersAndStrings() {
        assertEquals(9, MinMaxFinder.findMax(List.of(3, 9, 1, 7)));
        assertEquals("zebra", MinMaxFinder.findMax(List.of("apple", "zebra", "mango")));
        assertEquals(2, MinMaxFinder.countGreaterThan(List.of(10, 20, 30, 5), 15));
        assertThrows(IllegalArgumentException.class, () -> MinMaxFinder.findMax(List.<Integer>of()));
        assertThrows(IllegalArgumentException.class, () -> MinMaxFinder.countGreaterThan(List.of(1), null));
    }
}
