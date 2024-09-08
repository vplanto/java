package students;

import collections.lesson.students.CollectionExample;
import collections.lesson.students.StudentInformation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collection;
import java.util.ArrayList;

public class CollectionExampleTest {

    @Test
    public void testPrintAllStudents() {
        CollectionExample example = new CollectionExample();
        StudentInformation student1 = new StudentInformation("1001", "Alice");
        StudentInformation student2 = new StudentInformation("1002", "Bob");

        example.addStudent(student1);
        example.addStudent(student2);

        example.printAllStudents();
    }
}