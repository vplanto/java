package students;

import collections.lesson.students.Student;
import collections.lesson.students.StudentManager;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

public class ArrayListTest {

    @Test
    public void testArrayListImplementation() {
        List<Student> students = new ArrayList<>();
        StudentManager studentManager = new StudentManager(students);

        // Test addStudent
        studentManager.addStudent(new Student("John", 20));
        studentManager.addStudent(new Student("Alice", 22));

        // Test getAllStudents
        List<Student> retrievedStudents = studentManager.getAllStudents();
        assertEquals(2, retrievedStudents.size());

        // Test removeStudent
        studentManager.removeStudent(new Student("John", 20));
        retrievedStudents = studentManager.getAllStudents();
        assertEquals(2, retrievedStudents.size());
    }
}