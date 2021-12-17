package Ontology.Elements;

import jade.content.AgentAction;
import jade.core.AID;

public class OfferSwap implements AgentAction
{
    private AID offeringStudentAID;
    private Integer offerId;
    private Tutorial offeredTutorial;
    
    public AID getOfferingStudentAID() {
        return offeringStudentAID;
    }
    
    public void setOfferingStudentAID(AID offeringStudentAID) {
        this.offeringStudentAID = offeringStudentAID;
    }
    
    public Integer getOfferId() {
        return offerId;
    }
    
    public void setOfferId(Integer offerId) {
        this.offerId = offerId;
    }
    
    public Tutorial getOfferedTutorial() {
        return offeredTutorial;
    }
    
    public void setOfferedTutorial(Tutorial offeredTutorial) {
        this.offeredTutorial = offeredTutorial;
    }
}
