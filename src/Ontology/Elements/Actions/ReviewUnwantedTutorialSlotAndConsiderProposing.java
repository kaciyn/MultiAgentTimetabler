package Ontology.Elements.Actions;

import Ontology.Elements.Concepts.UnwantedTimeslotListing;
import jade.content.onto.annotations.Slot;

public class ReviewUnwantedTutorialSlotAndConsiderProposing
{
    public UnwantedTimeslotListing getUnwantedTimeslotListing() {
        return unwantedTimeslotListing;
    }
    
    public void setUnwantedTimeslotListing(UnwantedTimeslotListing unwantedTimeslotListing) {
        this.unwantedTimeslotListing = unwantedTimeslotListing;
    }
    
    @Slot(mandatory = true)
    private UnwantedTimeslotListing unwantedTimeslotListing;
}
