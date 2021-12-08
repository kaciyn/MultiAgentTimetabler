package Ontology.Elements;

import jade.content.Concept;

public class TimeslotId implements Concept
{
    public Integer timeslotID;
    
    public TimeslotId(Integer timeslotID) {
        this.timeslotID = timeslotID;
    }
    
    public Integer getTimeslotID() {
        return timeslotID;
    }
    
}
