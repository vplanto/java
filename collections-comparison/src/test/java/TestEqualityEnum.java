import org.collections.comparison.Fruit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestEqualityEnum{

@Test
public void listEquality() {
    List<Fruit> list = List.of(Fruit.BANANA, Fruit.BANANA, Fruit.APPLE, Fruit.CHERRY);

    // Equality of List is based on Type, Size, Contents, and Order
    // List allows duplicates
    // Order DOES impact the equality of a List
    Assertions.assertEquals(
            list,
            List.of(Fruit.BANANA, Fruit.BANANA, Fruit.APPLE, Fruit.CHERRY)
    );

    Assertions.assertNotEquals(
            list,
            List.of(Fruit.APPLE, Fruit.BANANA, Fruit.CHERRY)
    );
    Assertions.assertNotEquals(
            list,
            List.of(Fruit.CHERRY, Fruit.BANANA, Fruit.BANANA, Fruit.APPLE)
    );
}

    @Test
    public void setEquality() {
        Set<Fruit> set = Set.of(Fruit.BANANA, Fruit.APPLE, Fruit.CHERRY);

        // Equality of Set is based on Type, Size and Contents.
        // Set allows unique elements only.
        // Order DOES NOT impact the equality of a Set
        Assertions.assertEquals(
                set,
                Set.of(Fruit.APPLE, Fruit.BANANA, Fruit.CHERRY)
        );
        Assertions.assertEquals(
                set,
                Set.of(Fruit.CHERRY, Fruit.BANANA, Fruit.APPLE)
        );

        Assertions.assertNotEquals(
                set,
                Set.of(Fruit.APPLE, Fruit.BANANA)
        );
        Assertions.assertNotEquals(
                set,
                Set.of(Fruit.BANANA, Fruit.CHERRY)
        );
    }

    @Test
    public void mapEquality() {
        Map<Fruit, Integer> bag = new HashMap<>();
        bag.put(Fruit.APPLE, 1);
        bag.put(Fruit.BANANA, 2);
        bag.put(Fruit.CHERRY, 1);

        // Equality of Bag is based on Type, Size and Contents
        // Bag allows duplicates
        // Order DOES NOT impact the equality of a Bag
        Assertions.assertEquals(
                bag,
                Map.of(Fruit.BANANA, 2, Fruit.APPLE, 1, Fruit.CHERRY, 1)
        );
        Assertions.assertEquals(
                bag,
                Map.of(Fruit.CHERRY, 1, Fruit.BANANA, 2, Fruit.APPLE, 1)
        );

        Assertions.assertNotEquals(
                bag,
                Map.of(Fruit.APPLE, 2, Fruit.BANANA, 1, Fruit.CHERRY, 1)
        );
        Assertions.assertNotEquals(
                bag,
                Map.of(Fruit.APPLE, 1, Fruit.BANANA, 1, Fruit.CHERRY, 1)
        );
    }
}
