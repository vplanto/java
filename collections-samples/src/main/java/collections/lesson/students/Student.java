package collections.lesson.students;

public class Student {
    final private String name;
    final private int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Getters and setters

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object student){
        if (student == null){
            return false;
        }

        return this.age == ((Student)student).age && this.name.equals(((Student)student).name);
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}