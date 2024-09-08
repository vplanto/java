package students;

import collections.lesson.students.CollectionExample;
import collections.lesson.students.StudentInformation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TotalStudentsTest {

    @Test
    public void testTotalStudents() {
        CollectionExample example = new CollectionExample();
        StudentInformation student1 = new StudentInformation("1001", "Alice");
        StudentInformation student2 = new StudentInformation("1002", "Bob");

        example.addStudent(student1);
        example.addStudent(student2);

        assertEquals(2, example.getTotalStudents());
    }
}