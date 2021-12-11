package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Event implements Concept
{
    @Slot(mandatory = true)
    protected Integer timeslotId;
    
    public Event() {
    
    }
    
    public Event(Integer timeslotId) {
        this.timeslotId = timeslotId;
    }
    
    @Slot(mandatory = true)
    public Integer getTimeslotId() {
        return timeslotId;
    }
    
    public void setTimeSlotId(Integer timeslotId) {
        this.timeslotId = timeslotId;
    }
}
