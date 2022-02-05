package Ontology.Elements;

import jade.content.AgentAction;
import jade.core.AID;

public class OfferSwap implements AgentAction
{
    private AID offeringStudentAID;
    private Integer offerId;
    private Integer offeredTutorialSlot;
    
    public AID getOfferingStudentAID() {
        return offeringStudentAID;
    }
    
    public void setOfferingStudentAID(AID offeringStudentAID) {
        this.offeringStudentAID = offeringStudentAID;
    }
    
    public Integer getOfferId() {
        return offerId;
    }
    
    public void setOfferId(java.lang.Integer offerId) {
        this.offerId = offerId;
    }
    
    public Integer getOfferedTutorialSlot() {
        return offeredTutorialSlot;
    }
    
    public void setOfferedTutorialSlot(Integer offeredTutorialSlot) {
        this.offeredTutorialSlot = offeredTutorialSlot;
    }
}
