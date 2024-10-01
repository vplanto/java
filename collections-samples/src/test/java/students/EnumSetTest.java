package students;

import collections.lesson.students.StudentInformation;

import java.util.EnumSet;

public class EnumSetTest {

        // Enum to represent students
        public enum Students {
            STUDENT1(new StudentInformation("1", "Alice")),
            STUDENT2(new StudentInformation("2", "Bob"));

            private final StudentInformation studentInfo;

            Students(StudentInformation studentInfo) {
                this.studentInfo = studentInfo;
            }

            public StudentInformation getStudentInfo() {
                return studentInfo;
            }
        }

        public static void main(String[] args) {
            // Create an EnumSet and add students
            EnumSet<Students> studentSet = EnumSet.noneOf(Students.class);
            studentSet.add(Students.STUDENT1);
            studentSet.add(Students.STUDENT2);

            // Try to add the same students again
            studentSet.add(Students.STUDENT1);
            studentSet.add(Students.STUDENT2);

            // Validate the number of students in the set
            System.out.println("Number of students in the set: " + studentSet.size());

            // Print out the students
            for (Students student : studentSet) {
                System.out.println(student.getStudentInfo());
            }
        }

}
