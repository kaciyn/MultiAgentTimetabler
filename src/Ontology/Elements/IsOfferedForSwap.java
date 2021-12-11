package Ontology.Elements;

import jade.content.Predicate;
import jade.core.AID;

public class IsOfferedForSwap implements Predicate
{
    private AID studentAID;
    public Integer timeslotId;
    
    public Integer getTimeslotId() {
        return timeslotId;
    }
    
    public void setTimeslotId(Integer timeslotId) {
        this.timeslotId = timeslotId;
    }
    
    //todo CAN I JUST DO THIS??? WE'LL SEE!
    public void addTimeslotId(Integer timeslotId) {
        this.timeslotId=(timeslotId);
    }
    
    public AID getStudentAID() {
        return studentAID;
    }
    
    public void setStudentAID(AID studentAID) {
        this.studentAID = studentAID;
    }

//    public boolean isOnOffer() {
//        return isOnOffer;
//    }
//
//    public void setOnOffer(boolean onOffer) {
//        isOnOffer = onOffer;
//    }
}
