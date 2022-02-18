package Ontology.Elements;

import jade.content.AgentAction;

public class OfferSwap implements AgentAction
{
    private Long offerId;
    private Long offeredTutorialSlot;
    
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
