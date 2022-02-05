package Ontology.Elements;

import jade.content.Predicate;

public class IsSwapResult implements Predicate
{
    private Integer offeredTutorialSlot;
    private Integer requestedTutorialSlot;
    
    public Integer getOfferedTutorialSlot() {
        return offeredTutorialSlot;
    }
    
    public void setOfferedTutorialSlot(Integer offeredTutorialSlot) {
        this.offeredTutorialSlot = offeredTutorialSlot;
    }
    
    public Integer getRequestedTutorialSlot() {
        return requestedTutorialSlot;
    }
    
    public void setRequestedTutorialSlot(Integer requestedTutorialSlot) {
        this.requestedTutorialSlot = requestedTutorialSlot;
    }
}
