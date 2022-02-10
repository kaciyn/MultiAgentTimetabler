package Ontology.Elements;

import jade.content.Predicate;

public class IsSwapResult implements Predicate
{
    private Long offeredTutorialSlot;
    private Long requestedTutorialSlot;
    
    public Long getOfferedTutorialSlot() {
        return offeredTutorialSlot;
    }
    
    public void setOfferedTutorialSlot(Long offeredTutorialSlot) {
        this.offeredTutorialSlot = offeredTutorialSlot;
    }
    
    public Long getRequestedTutorialSlot() {
        return requestedTutorialSlot;
    }
    
    public void setRequestedTutorialSlot(Long requestedTutorialSlot) {
        this.requestedTutorialSlot = requestedTutorialSlot;
    }
}
