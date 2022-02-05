package Ontology.Elements;

import jade.content.AgentAction;

public class AcceptSwap implements AgentAction
{
    private Integer unwantedTutorialSlot;
    private Integer proposedTutorialSlot;
    
    public Integer getUnwantedTutorialSlot() {
        return unwantedTutorialSlot;
    }
    
    public void setUnwantedTutorialSlot(Integer unwantedTutorialSlot) {
        this.unwantedTutorialSlot = unwantedTutorialSlot;
    }
    
    public Integer getProposedTutorialSlot() {
        return proposedTutorialSlot;
    }
    
    public void setProposedTutorialSlot(Integer proposedTutorialSlot) {
        this.proposedTutorialSlot = proposedTutorialSlot;
    }
}
