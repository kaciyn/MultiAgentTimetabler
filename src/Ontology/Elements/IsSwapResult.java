package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

public class IsSwapResult implements Predicate
{
    public SwapProposal getSwapProposal() {
        return swapProposal;
    }
    
    public void setSwapProposal(SwapProposal swapProposal) {
        this.swapProposal = swapProposal;
    }
    
    public boolean isAccepted() {
        return accepted;
    }
    
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
    
    @Slot(mandatory = true)
    private SwapProposal swapProposal;
    
    @Slot(mandatory = true)
    private boolean accepted;
}
