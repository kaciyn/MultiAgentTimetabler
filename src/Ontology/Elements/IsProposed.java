package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class IsProposed implements Predicate
{
    
    public SwapProposal getSwapProposal() {
        return swapProposal;
    }
    
    public void setSwapProposal(SwapProposal swapProposal) {
        this.swapProposal = swapProposal;
    }
    
    @Slot(mandatory = true)
    private SwapProposal swapProposal;
    
    public AID getProposer() {
        return proposer;
    }
    
    public void setProposer(AID proposer) {
        this.proposer = proposer;
    }
    
    private AID proposer;
}
