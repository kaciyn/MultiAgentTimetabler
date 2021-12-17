package Ontology.Elements;

import jade.content.AgentAction;

public class AcceptSwap implements AgentAction
{
    private Tutorial unwantedTutorial;
    private Tutorial proposedTutorial;
    
    public Tutorial getUnwantedTutorial() {
        return unwantedTutorial;
    }
    
    public void setUnwantedTutorial(Tutorial unwantedTutorial) {
        this.unwantedTutorial = unwantedTutorial;
    }
    
    public Tutorial getProposedTutorial() {
        return proposedTutorial;
    }
    
    public void setProposedTutorial(Tutorial proposedTutorial) {
        this.proposedTutorial = proposedTutorial;
    }
}
