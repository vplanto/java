import org.collections.comparison.Fruit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class TestConvertEnum {

    @Test
    public void convertToSortedListForEquality() {
        List<Fruit> list = List.of(Fruit.BANANA, Fruit.BANANA, Fruit.APPLE, Fruit.CHERRY);

        // Sorting the List we can more predictably compare the contents
        List<Fruit> sortedList = list.stream().sorted().toList();

        Assertions.assertEquals(
                sortedList,
                List.of(Fruit.APPLE, Fruit.BANANA, Fruit.BANANA, Fruit.CHERRY));

        Assertions.assertNotEquals(
                sortedList,
                List.of(Fruit.CHERRY, Fruit.BANANA, Fruit.CHERRY, Fruit.APPLE));
    }

    @Test
    public void convertToSetForEquality() {
        List<Fruit> list = List.of(Fruit.BANANA, Fruit.BANANA, Fruit.APPLE, Fruit.CHERRY);

        // Using a Set we can test unique contents without caring for order
        Set<Fruit> set = new HashSet<>(list);

        Assertions.assertEquals(
                set,
                Set.of(Fruit.CHERRY, Fruit.BANANA, Fruit.APPLE)
        );
        Assertions.assertEquals(
                set,
                Set.of(Fruit.APPLE, Fruit.BANANA, Fruit.CHERRY)
        );

        Assertions.assertNotEquals(
                set,
                Set.of(Fruit.APPLE, Fruit.BANANA)
        );
    }

    @Test
    public void convertToBagForEquality() {
        List<Fruit> list = List.of(Fruit.BANANA, Fruit.BANANA, Fruit.APPLE, Fruit.CHERRY);

        // Using a Bag we can test size and contents without caring about order
        // Create a new HashMap to act as a Bag, where the key is the Fruit and the value is the count of that Fruit
        Map<Fruit, Integer> bag = new HashMap<>();

        // Iterate over each Fruit in the list
        for (Fruit fruit : list) {
            // For each Fruit, update the count in the bag
            // If the Fruit is already in the bag, increment its count by 1
            // If the Fruit is not in the bag, add it with an initial count of 1
            bag.put(fruit, bag.getOrDefault(fruit, 0) + 1);
        }


        Map<Fruit, Integer> expectedBag1 = new HashMap<>();
        expectedBag1.put(Fruit.CHERRY, 1);
        expectedBag1.put(Fruit.BANANA, 2);
        expectedBag1.put(Fruit.APPLE, 1);

        Map<Fruit, Integer> expectedBag2 = new HashMap<>();
        expectedBag2.put(Fruit.CHERRY, 1);
        expectedBag2.put(Fruit.BANANA, 2);
        expectedBag2.put(Fruit.APPLE, 1);

        Map<Fruit, Integer> notExpectedBag = new HashMap<>();
        notExpectedBag.put(Fruit.CHERRY, 1);
        notExpectedBag.put(Fruit.BANANA, 1);
        notExpectedBag.put(Fruit.APPLE, 1);

        Assertions.assertEquals(bag, expectedBag1);
        Assertions.assertEquals(bag, expectedBag2);
        Assertions.assertNotEquals(bag, notExpectedBag);
    }
}
