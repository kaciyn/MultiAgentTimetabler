package Ontology.Elements;

import jade.content.Concept;
import jade.core.AID;

public class UnwantedTimeslot implements Concept
{
    private AID studentAID;
    private int timeslotID;
    
    public UnwantedTimeslot(AID studentAID, int timeslotID) {
        this.studentAID = studentAID;
        this.timeslotID = timeslotID;
    }
    
    public int getTimeslotID() {
        return timeslotID;
    }
    
    public void setTimeslotID(int timeslotID) {
        this.timeslotID = timeslotID;
    }
    
    
    public AID getStudentAID() {
        return studentAID;
    }
    
    public void setStudentAID(AID studentAID) {
        this.studentAID = studentAID;
    }
    
 
}
