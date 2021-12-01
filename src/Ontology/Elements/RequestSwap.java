package Ontology.Elements;

import jade.content.AgentAction;
import jade.core.AID;

public class RequestSwap implements AgentAction
{
    private AID studentAgent;
    private Tutorial unwantedTutorial;
    
    public AID getStudentAgent() {
        return studentAgent;
    }
    
    public void setStudentAgent(AID studentAgent) {
        this.studentAgent = studentAgent;
    }
    
    public Tutorial getUnwantedTutorial() {
        return unwantedTutorial;
    }
    
    public void setUnwantedTutorial(Tutorial unwantedTutorial) {
        this.unwantedTutorial = unwantedTutorial;
    }
}
