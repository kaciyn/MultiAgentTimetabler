package Ontology.Elements.Predicates;

import jade.core.AID;

public class IsLockedForOffer
{
    private AID studentAID;
    private boolean isLocked;
    
    public int getTimeslotID() {
        return timeslotID;
    }
    
    public void setTimeslotID(int timeslotID) {
        this.timeslotID = timeslotID;
    }
    
    public int timeslotID;
    
    public AID getStudentAID() {
        return studentAID;
    }
    
    public void setStudentAID(AID studentAID) {
        this.studentAID = studentAID;
    }
    
    public boolean isLocked() {
        return isLocked;
    }
    
    public void setLocked(boolean locked) {
        isLocked = locked;
    }
}
