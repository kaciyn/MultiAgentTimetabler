package Ontology.Elements.Actions;

import Ontology.Elements.Concepts.TutorialSlot;
import jade.content.AgentAction;
import jade.core.AID;

public class ListUnwantedSlot implements AgentAction
{
    
    private AID requestingStudentAgent;
    private TutorialSlot unwantedTutorialSlot;
    
    public AID getRequestingStudentAgent() {
        return requestingStudentAgent;
    }
    
    public void setRequestingStudentAgent(AID requestingStudentAgent) {
        this.requestingStudentAgent = requestingStudentAgent;
    }
    
    public TutorialSlot getUnwantedTutorialSlot() {
        return unwantedTutorialSlot;
    }
    
    public void setUnwantedTutorialSlot(TutorialSlot unwantedTutorialSlot) {
        this.unwantedTutorialSlot = unwantedTutorialSlot;
    }
    
}
