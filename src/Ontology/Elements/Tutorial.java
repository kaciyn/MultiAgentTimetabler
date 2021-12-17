package Ontology.Elements;

import java.util.ArrayList;

public class Tutorial extends ModuleEvent
{
    private ArrayList<Student> studentsAttending;
    
    public ArrayList<Student> getStudents() {
        return studentsAttending;
    }
    
    public void setStudents(ArrayList<Student> students) {
        this.studentsAttending = students;
    }
    
    public void addStudent(Student student) {
        this.studentsAttending.add(student);
    }
    
}
