package Ontology.Elements.Concepts;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.time.DayOfWeek;
import java.util.ArrayList;

public class Timeslot implements Concept
{
    @Slot(mandatory = true)
    public int timeslotID;
    
    private DayOfWeek day;
    
    private int startHour;

//    //timeslot locked for swaps
//    private boolean isLocked;
    
    public int getTimeslotID() {
        return timeslotID;
    }
    
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
    
    public int getStartHour()
    {
        return (this.timeslotID % 10) + 8;
    }
//
//    public boolean isLocked() {
//        return isLocked;
//    }
//
//    public void setLocked(boolean locked) {
//        isLocked = locked;
//    }
}
