package Ontology.Elements.Concepts;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class SwapProposal implements Concept
{
    //can be null when student is proposing
    private Long proposalId;
    
    @Slot(mandatory = true)
    private Long unwantedListingId;
    
    @Slot(mandatory = true)
    private TutorialSlot proposedSlot;
    
    public Long getProposalId() {
        return proposalId;
    }
    
    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }
    
    public Long getUnwantedListingId() {
        return unwantedListingId;
    }
    
    public void setUnwantedListingId(Long unwantedListingId) {
        this.unwantedListingId = unwantedListingId;
    }
    
    public TutorialSlot getProposedSlot() {
        return proposedSlot;
    }
    
    public void setProposedSlot(TutorialSlot proposedSlotOffer) {
        this.proposedSlot = proposedSlotOffer;
    }
    
}
