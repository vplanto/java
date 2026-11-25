package ua.edu.onu.java.sem1.t07_solid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SolidDesignTest {

    @Test
    void processorDelegatesDiscountAndNotification() {
        List<String> sent = new ArrayList<>();
        DiscountPolicy tenPercentOff = amount -> amount - (amount / 10);
        Notifier fakeNotifier = (recipient, msg) -> sent.add(recipient + "|" + msg);

        OrderProcessor processor = new OrderProcessor(tenPercentOff, fakeNotifier);
        long total = processor.checkout("ORD-1", "alice@example.com", 2000);

        assertEquals(1800, total);
        assertEquals(List.of("alice@example.com|Order ORD-1 total: 1800"), sent);
    }

    @Test
    void validatesArgumentsAndConstructorDependencies() {
        assertThrows(IllegalArgumentException.class, () -> new OrderProcessor(null, (r, m) -> {}));
        OrderProcessor processor = new OrderProcessor(a -> a, (r, m) -> {});
        assertThrows(IllegalArgumentException.class, () -> processor.checkout("", "u", 100));
        assertThrows(IllegalArgumentException.class, () -> processor.checkout("1", "u", -1));
    }
}
