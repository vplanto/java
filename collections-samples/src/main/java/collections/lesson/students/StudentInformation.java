package collections.lesson.students;

public class StudentInformation {
    private String id;
    private String name;

    public StudentInformation(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StudentInformation that = (StudentInformation) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}