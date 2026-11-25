package ua.edu.onu.java.sem1.t10_patterns;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatternsTest {

    @Test
    void factoryAndDecoratorComposeCleanly() {
        TextFormatter trim = FormatterFactory.create("trim");
        TextFormatter upper = FormatterFactory.create("upper");
        assertEquals("hello", trim.format("  hello  "));
        assertEquals("HELLO", upper.format("hello"));

        TextFormatter pipeline = FormatterFactory.withPrefix(upper, "[LOG] ");
        assertEquals("[LOG] ERROR", pipeline.format("error"));

        assertThrows(IllegalArgumentException.class, () -> FormatterFactory.create("unknown"));
        assertThrows(IllegalArgumentException.class, () -> FormatterFactory.create(null));
    }
}
