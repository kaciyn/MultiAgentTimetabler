package Ontology.Elements;

import jade.content.AgentAction;
import jade.core.AID;

public class OfferSwap implements AgentAction
{
    private Long offerId;
    private Long offeredTutorialSlot;
    
    private AID offeringStudent;
    
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
    
    public AID getOfferingStudent() {
        return offeringStudent;
    }
    
    public void setOfferingStudent(AID offeringStudent) {
        this.offeringStudent = offeringStudent;
    }
}
