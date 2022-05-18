package Ontology.Elements;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

///student -> timetabler
public class ProposeSwapToTimetabler implements AgentAction
{
    @Slot(mandatory = true)
    private UnwantedTimeslotListing unwantedTimeslotListing;
    ;
    
    public UnwantedTimeslotListing getUnwantedTimeslotListing() {
        return unwantedTimeslotListing;
    }
    
    public void setUnwantedTimeslotListing(UnwantedTimeslotListing unwantedTimeslotListing) {
        this.unwantedTimeslotListing = unwantedTimeslotListing;
    }
    
    //unsure if the slot restriction is taken into account when in cfp?
    public TutorialSlot getProposedSlot() {
        return proposedSlot;
    }
    
    public void setProposedSlot(TutorialSlot proposedSlot) {
        this.proposedSlot = proposedSlot;
    }
    
    @Slot(mandatory = true)
    private TutorialSlot proposedSlot;
    
    @Slot(mandatory = true)
    private AID proposer;
    
    public AID getProposer() {
        return proposer;
    }
    
    public void setProposer(AID getProposer) {
        this.proposer = getProposer;
    }
    
}
