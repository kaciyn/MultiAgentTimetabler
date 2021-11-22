package Ontology.Elements.Concepts;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;

public class Event implements Concept
{
    @Slot(mandatory = true)
    private int timeslotID;
    
    public int getTimeslotID() {
        return timeslotID;
    }
    
    public void setTimeslotID(int timeslotID) {
        this.timeslotID = timeslotID;
    }
}
