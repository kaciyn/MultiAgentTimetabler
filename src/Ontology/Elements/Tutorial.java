package Ontology.Elements;

import java.util.ArrayList;

public class Tutorial extends ModuleEvent
{
    protected int capacity;
    
    private ArrayList<Student> students;
    
    public Tutorial(String moduleId, int capacity) {
        super(moduleId);
        this.capacity = capacity;
    }
    
    public Tutorial(String moduleId) {
        super(moduleId);
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public ArrayList<Student> getStudents() {
        return students;
    }
    
    public void setStudents(ArrayList<Student> students) {
        this.students = students;
    }
}
