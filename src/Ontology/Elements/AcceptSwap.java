package Ontology.Elements;

import jade.content.AgentAction;

public class AcceptSwap implements AgentAction
{
    private Long unwantedTutorialSlot;
    private Long proposedTutorialSlot;
    
    private Long offerId;
    
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
