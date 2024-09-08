package students;

import collections.lesson.students.CollectionExample;
import collections.lesson.students.StudentInformation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RemoveStudentTest {

    @Test
    public void testRemoveStudent() {
        CollectionExample example = new CollectionExample();
        StudentInformation student = new StudentInformation("1001", "Alice");

        example.addStudent(student);
        example.removeStudent(student);
        assertFalse(example.containsStudent(student));
    }
}