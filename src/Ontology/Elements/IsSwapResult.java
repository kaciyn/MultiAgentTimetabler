package Ontology.Elements;

import jade.content.Predicate;

public class IsSwapResult implements Predicate
{
    private Tutorial offeredTutorial;
    private Tutorial requestedTutorial;
    
    public Tutorial getOfferedTutorial() {
        return offeredTutorial;
    }
    
    public void setOfferedTutorial(Tutorial offeredTutorial) {
        this.offeredTutorial = offeredTutorial;
    }
    
    public Tutorial getRequestedTutorial() {
        return requestedTutorial;
    }
    
    public void setRequestedTutorial(Tutorial requestedTutorial) {
        this.requestedTutorial = requestedTutorial;
    }
}
