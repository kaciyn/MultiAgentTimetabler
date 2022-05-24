package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;
//created by timetabler, akin to a listing
public class SwapProposal implements Concept
{
    @Slot(mandatory = true)
    private Long proposalId;
    
    @Slot(mandatory = true)
    private Long unwantedListingId;
    
    @Slot(mandatory = true)
    private TutorialSlot proposedSlot;
    
    public SwapProposal() {
    
    }
    
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
    
    public SwapProposal(Long proposalId, Long unwantedListingId, TutorialSlot proposedSlot) {
        this.proposalId = proposalId;
        this.unwantedListingId = unwantedListingId;
        this.proposedSlot = proposedSlot;
    }
    
 
}
