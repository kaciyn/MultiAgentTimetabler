package Ontology.Elements;

import jade.content.Predicate;
import jade.core.AID;

public class IsListedForOffer implements Predicate
{
    private AID studentAID;
    private boolean isOnOffer;
    public int timeslotID;
    
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
    
    public boolean isOnOffer() {
        return isOnOffer;
    }
    
    public void setOnOffer(boolean onOffer) {
        isOnOffer = onOffer;
    }
}
