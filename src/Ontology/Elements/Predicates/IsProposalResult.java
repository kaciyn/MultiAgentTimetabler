package Ontology.Elements.Predicates;

import Ontology.Elements.Concepts.SwapProposal;
import jade.core.AID;

public class IsProposalResult
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
    
    public boolean isAccepted() {
        return accepted;
    }
    
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
    
    private boolean accepted;
}
