package Ontology.Elements;

import jade.content.onto.annotations.Slot;

import java.time.DayOfWeek;
import java.util.ArrayList;

public class Tutorial extends ModuleEvent
{
    @Slot(mandatory = true)
    protected Integer capacity;
    
    private ArrayList<Student> students;
    
    @Slot(mandatory = true)
    private Integer timeslotID;
    
    @Slot(mandatory = true)
    private DayOfWeek day;
    
    public Tutorial(String moduleId, int capacity) {
        super(moduleId);
        this.capacity = capacity;
    }
    
    public Tutorial(String moduleId) {
        super(moduleId);
    }
    
    @Slot(mandatory = true)
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
    
    
    @Slot(mandatory = true)
    private Integer startHour;
    
    @Slot(mandatory = true)
    public int getTimeslotID() {
        return timeslotID;
    }
    
    @Slot(mandatory = true)
    public DayOfWeek getDay() {
        switch (this.timeslotID / 10) {
            case 0:
                return DayOfWeek.MONDAY;
            case 1:
                return DayOfWeek.TUESDAY;
            case 2:
                return DayOfWeek.WEDNESDAY;
            case 3:
                return DayOfWeek.THURSDAY;
            case 4:
                return DayOfWeek.FRIDAY;
            default:
                throw new IllegalArgumentException("Invalid timeslotId");
        }
    }
    
    @Slot(mandatory = true)
    public int getStartHour()
    {
        this.startHour= (this.timeslotID % 10) + 8;
        return this.startHour;
    }
    
}
