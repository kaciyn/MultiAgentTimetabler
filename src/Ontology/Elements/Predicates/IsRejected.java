package Ontology.Elements.Predicates;

import Ontology.Elements.Concepts.SwapProposal;
import jade.core.AID;

public class IsRejected
{
    public SwapProposal getSwapProposal() {
        return swapProposal;
    }
    
    public void setSwapProposal(SwapProposal swapProposal) {
        this.swapProposal = swapProposal;
    }
    
    private SwapProposal swapProposal;
    
    public AID getProposer() {
        return proposer;
    }
    
    public void setProposer(AID proposer) {
        this.proposer = proposer;
    }
    
    private AID proposer;
}
