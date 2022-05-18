package Ontology.Elements;

import jade.content.AgentAction;
import jade.core.AID;

public class ListUnwantedSlot implements AgentAction
{
    //unsure if it'd be necessary to have the timetabler agent in here too since they're the one to be performing the action? but i feel like it's already very clear
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
