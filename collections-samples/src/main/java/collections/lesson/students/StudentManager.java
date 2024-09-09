package collections.lesson.students;

import java.util.List;

public class StudentManager {
    private List<Student> students;

    public StudentManager(List<Student> students) {
        this.students = students;
    }

    public void addStudent(Student student) {
        students.add(student);
    }

    public void removeStudent(Student student) {
        students.remove(student);
    }

    public List<Student> getAllStudents() {
        return students;
    }
}