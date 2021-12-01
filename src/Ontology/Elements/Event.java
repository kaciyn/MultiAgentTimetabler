package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Event implements Concept
{
    @Slot(mandatory = true)
    protected int timeslotId;
    
    
    public Event() {
    
    }
    
    public Event(int timeslotId) {
        this.timeslotId = timeslotId;
    }
    
    @Slot(mandatory = true)
    public int getTimeslotId() {
        return timeslotId;
    }
    
    public void setTimeSlotId(int timeslotId) {
        this.timeslotId = timeslotId;
    }
}
