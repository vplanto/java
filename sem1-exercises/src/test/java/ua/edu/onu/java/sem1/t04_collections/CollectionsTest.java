package ua.edu.onu.java.sem1.t04_collections;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CollectionsTest {

    @Test
    void simpleLinkedListBasicOperations() {
        SimpleLinkedList<String> list = new SimpleLinkedList<>();
        assertTrue(list.isEmpty());
        list.addLast("A");
        list.addLast("B");
        list.addLast("C");

        assertEquals(3, list.size());
        assertEquals("A", list.get(0));
        assertEquals("C", list.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));

        assertEquals("A", list.removeFirst());
        assertEquals("B", list.removeFirst());
        assertEquals("C", list.removeFirst());
        assertTrue(list.isEmpty());
        assertThrows(NoSuchElementException.class, list::removeFirst);
    }

    @Test
    void gradeBookAggregatesListSetAndMap() {
        GradeBook book = new GradeBook();
        book.addGrade("Olena", 90);
        book.addGrade("Ivan", 80);
        book.addGrade("Olena", 100);

        assertEquals(3, book.getHistory().size());
        assertEquals(Set.of("Olena", "Ivan"), book.getUniqueStudents());
        Map<String, Double> avg = book.getAverageByStudent();
        assertEquals(95.0, avg.get("Olena"), 0.001);
        assertEquals(80.0, avg.get("Ivan"), 0.001);

        assertThrows(IllegalArgumentException.class, () -> book.addGrade("Olena", 101));
        assertThrows(IllegalArgumentException.class, () -> book.addGrade("", 50));
    }
}
