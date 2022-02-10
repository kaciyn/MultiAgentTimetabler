package Ontology.Elements;

import jade.content.AgentAction;
import jade.core.AID;

public class OfferSwap implements AgentAction
{
    private AID offeringStudentAID;
    private Long offerId;
    private Long offeredTutorialSlot;
    
    public AID getOfferingStudentAID() {
        return offeringStudentAID;
    }
    
    public void setOfferingStudentAID(AID offeringStudentAID) {
        this.offeringStudentAID = offeringStudentAID;
    }
    
    public Long getOfferId() {
        return offerId;
    }
    
    public void setOfferId(java.lang.Long offerId) {
        this.offerId = offerId;
    }
    
    public Long getOfferedTutorialSlot() {
        return offeredTutorialSlot;
    }
    
    public void setOfferedTutorialSlot(Long offeredTutorialSlot) {
        this.offeredTutorialSlot = offeredTutorialSlot;
    }
}
