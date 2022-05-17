package Ontology.Elements.Actions;

import Ontology.Elements.Concepts.SwapProposal;
import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

///timetabler -> student
public class ProposeSwapToStudent implements AgentAction
{
    public SwapProposal getSwapProposal() {
        return swapProposal;
    }
    
    public void setSwapProposal(SwapProposal swapProposal) {
        this.swapProposal = swapProposal;
    }
    
    @Slot(mandatory = true)
    private SwapProposal swapProposal;
    
    @Slot(mandatory = true)
    public AID getStudent() {
        return student;
    }
    
    public void setStudent(AID student) {
        this.student = student;
    }
    
    @Slot(mandatory = true)
    private AID student;
}

