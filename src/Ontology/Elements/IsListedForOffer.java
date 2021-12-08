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
    
    //todo CAN I JUST DO THIS??? WE'LL SEE!
    public void addTimeslotId(TimeslotId timeslotId) {
        this.timeslotIds.add(timeslotId);
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
