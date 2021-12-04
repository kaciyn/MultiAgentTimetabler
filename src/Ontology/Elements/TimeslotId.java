package Ontology.Elements;

import jade.content.Concept;

public class TimeslotId implements Concept
{
    public int timeslotID;
    
    public TimeslotId(int timeslotID) {
        this.timeslotID = timeslotID;
    }
    
    public int getTimeslotID() {
        return timeslotID;
    }
    
}
