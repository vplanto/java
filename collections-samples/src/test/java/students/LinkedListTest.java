package students;

import collections.lesson.students.Student;
import collections.lesson.students.StudentManager;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinkedListTest {

    @Test
    public void testArrayListImplementation() {
        List<Student> students = new LinkedList<>();
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