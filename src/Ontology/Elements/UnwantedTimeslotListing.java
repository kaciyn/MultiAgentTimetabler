package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class UnwantedTimeslotListing implements Concept
{
    public UnwantedTimeslotListing() {
    
    }
    
    public UnwantedTimeslotListing(TutorialSlot tutorialSlot, Long unwantedListingId) {
        this.tutorialSlot = tutorialSlot;
        this.unwantedListingId = unwantedListingId;
    }
    
    public Long getUnwantedListingId() {
        return unwantedListingId;
    }
    
    public void setUnwantedListingId(Long unwantedListingId) {
        this.unwantedListingId = unwantedListingId;
    }
    
    public TutorialSlot getTutorialSlot() {
        return tutorialSlot;
    }
    
    public void setTutorialSlot(TutorialSlot tutorialSlot) {
        this.tutorialSlot = tutorialSlot;
    }
    
    @Slot(mandatory = true)
    private Long unwantedListingId;
    
    @Slot(mandatory = true)
    private TutorialSlot tutorialSlot;
    
}
