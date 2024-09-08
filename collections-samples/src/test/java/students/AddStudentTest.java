package students;

import collections.lesson.students.CollectionExample;
import collections.lesson.students.StudentInformation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AddStudentTest {

    @Test
    public void testAddStudent() {
        CollectionExample example = new CollectionExample();
        StudentInformation student = new StudentInformation("1001", "Alice");

        example.addStudent(student);
        assertTrue(example.containsStudent(student));
    }
}