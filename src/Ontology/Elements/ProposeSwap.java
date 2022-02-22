package Ontology.Elements;

import jade.content.AgentAction;

public class ProposeSwap implements AgentAction
{
    private Long offerId;
    
    private Long unwantedTutorialSlot;
    private Long proposedTutorialSlot;
    
    public Long getUnwantedTutorialSlot() {
        return unwantedTutorialSlot;
    }
    
    public void setUnwantedTutorialSlot(Long unwantedTutorialSlot) {
        this.unwantedTutorialSlot = unwantedTutorialSlot;
    }
    
    public Long getProposedTutorialSlot() {
        return proposedTutorialSlot;
    }
    
    public void setProposedTutorialSlot(Long proposedTutorialSlot) {
        this.proposedTutorialSlot = proposedTutorialSlot;
    }
    
    public Long getOfferId() {
        return offerId;
    }
    
    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }}
