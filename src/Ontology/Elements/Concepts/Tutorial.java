package Ontology.Elements.Concepts;

import java.util.ArrayList;

public class Tutorial extends Event
{
    private String moduleId;
    
    private int timeSlotId;
    
    private int capacity;
    
    private ArrayList<String> students;
    
    public Tutorial(String moduleId, int capacity) {
        this.moduleId = moduleId;
        this.capacity = capacity;
    }
    
    public int getModuleId()
    {
        return moduleId;
    }

    public void setModuleId(int moduleId)
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
    
    public ArrayList<String> getStudents() {
        return students;
    }
    
    public void setStudents(ArrayList<String> students) {
        this.students = students;
    }
}
