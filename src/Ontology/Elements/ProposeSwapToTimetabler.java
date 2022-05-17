package Ontology.Elements;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

///student -> timetabler
public class ProposeSwapToTimetabler implements AgentAction
{
    @Slot(mandatory = true)
    private SwapProposal swapProposal;
    
    @Slot(mandatory = true)
    private AID proposer;
    
    public SwapProposal getSwapProposal() {
        return swapProposal;
    }
    
    public void setSwapProposal(SwapProposal swapProposal) {
        this.swapProposal = swapProposal;
    }
    
    public AID getProposer() {
        return proposer;
    }
    
    public void setProposer(AID getProposer) {
        this.proposer = getProposer;
    }
    
}
