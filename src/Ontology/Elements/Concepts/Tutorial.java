package Ontology.Elements.Concepts;

import java.util.ArrayList;

public class Tutorial extends Event
{
    private String moduleId;
    
    private int timeSlotId;
    
    private int capacity;
    
    private ArrayList<Student> students;
    
    public Tutorial(String moduleId, int capacity) {
        this.moduleId = moduleId;
        this.capacity = capacity;
    }
    
    public Tutorial(String moduleId) {
        this.moduleId = moduleId;
    }
    
    public String getModuleId()
    {
        return moduleId;
    }

    public void setModuleId(String moduleId)
    {
        this.moduleId = moduleId;
    }
    
    public int getTimeSlotId() {
        return timeSlotId;
    }
    
    public void setTimeSlotId(int timeSlotId) {
        this.timeSlotId = timeSlotId;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public ArrayList<Module> getStudents() {
        return students;
    }
    
    public void setStudents(ArrayList<Student> students) {
        this.students = students;
    }
}
