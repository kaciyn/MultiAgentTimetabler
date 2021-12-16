package Ontology.Elements;

import jade.content.AgentAction;
import jade.core.AID;

public class OfferSwap implements AgentAction
{
    private AID offeringStudentAID;
    private Integer timeslotId;
    private Integer timeslotRequestId;
    
    public Integer getTimeslotRequestId() {
        return timeslotRequestId;
    }
    
    public void setTimeslotRequestId(Integer timeslotRequestId) {
        this.timeslotRequestId = timeslotRequestId;
    }
    
    public Integer getTimeslotId() {
        return timeslotId;
    }
    
    public void setTimeslotId(Integer timeslotId) {
        this.timeslotId = timeslotId;
    }
    
    public AID getOfferingStudentAID() {
        return offeringStudentAID;
    }
    
    public void setOfferingStudentAID(AID offeringStudentAID) {
        this.offeringStudentAID = offeringStudentAID;
    }
    
}
