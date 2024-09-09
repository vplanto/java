package students;

import collections.lesson.students.StudentInformation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.LinkedHashSet;

public class LinkedHashSetTest {

    @Test
    public void testLinkedHashSet() {
        Set<StudentInformation> studentSet = new LinkedHashSet<>();

        StudentInformation student1 = new StudentInformation("1001", "Alice");
        StudentInformation student2 = new StudentInformation("1002", "Bob");

        assertTrue(studentSet.add(student1));
        assertTrue(studentSet.add(student2));
        assertFalse(studentSet.add(student1)); // Duplicate element, should return false

        assertEquals(2, studentSet.size());
    }
}