package Ontology.Elements;

import jade.content.Predicate;

import java.util.ArrayList;

public class IsListedForOffer implements Predicate
{
//    private AID studentAID;
    public ArrayList<TimeslotId> timeslotIds;
    
    public ArrayList<TimeslotId> getTimeslotIds() {
        return timeslotIds;
    }
    
    public void setTimeslotIds(ArrayList<TimeslotId> timeslotIds) {
        this.timeslotIds = timeslotIds;
    }
    
    
//    public AID getStudentAID() {
//        return studentAID;
//    }
//
//    public void setStudentAID(AID studentAID) {
//        this.studentAID = studentAID;
//    }
    
//    public boolean isOnOffer() {
//        return isOnOffer;
//    }
//
//    public void setOnOffer(boolean onOffer) {
//        isOnOffer = onOffer;
//    }
}
