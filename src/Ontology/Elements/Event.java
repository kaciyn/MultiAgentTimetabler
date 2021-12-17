package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.time.DayOfWeek;

public class Event implements Concept
{
    @Slot(mandatory = true)
    protected Integer capacity;
    
    @Slot(mandatory = true)
    protected Integer timeslotId;

    @Slot(mandatory = true)
    public Integer getTimeslotId() {
        return timeslotId;
    }
    
    public void setTimeSlotId(Integer timeslotId) {
        this.timeslotId = timeslotId;
    }
    
    @Slot(mandatory = true)
    private Integer startHour;
    
    @Slot(mandatory = true)
    private DayOfWeek day;
    
    @Slot(mandatory = true)
    public DayOfWeek getDay() {
        switch (this.timeslotId / 10) {
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
        this.startHour = (this.timeslotId % 10) + 8;
        return this.startHour;
    }
}
