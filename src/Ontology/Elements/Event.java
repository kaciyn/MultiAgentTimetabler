package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.time.DayOfWeek;

public class Event implements Concept
{
 
    protected Long capacity;
    
    protected Long timeslotId;
    
    private Long startHour;
    
    private DayOfWeek day;
    
    @Slot(mandatory = true)
    public Long getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }
    
    @Slot(mandatory = true)
    public Long getTimeslotId() {
        return timeslotId;
    }
    
    public void setTimeSlotId(Long timeslotId) {
        this.timeslotId = timeslotId;
    }
    
    @Slot(mandatory = true)
    public DayOfWeek getDay() {
        switch ((int) (this.timeslotId / 10)) {
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
    public Long getStartHour()
    {
        this.startHour = (this.timeslotId % 10) + 8;
        return this.startHour;
    }
}
