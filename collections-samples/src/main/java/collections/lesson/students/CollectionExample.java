package collections.lesson.students;

import java.util.Collection;
import java.util.ArrayList;

public class CollectionExample {

    private Collection<StudentInformation> students;

    public CollectionExample() {
        students = new ArrayList<>();
    }

    public void addStudent(StudentInformation student) {
        students.add(student);
    }

    public void removeStudent(StudentInformation student) {
        students.remove(student);
    }

    public void printAllStudents() {
        System.out.println("All Students:");
        for (StudentInformation student : students) {
            System.out.println("Student ID: " + student.getId() + ", Name: " + student.getName());
        }
    }

    public int getTotalStudents() {
        return students.size();
    }

    public boolean containsStudent(StudentInformation student) {
        return students.contains(student);
    }

    public void clearAllStudents() {
        students.clear();
    }
}