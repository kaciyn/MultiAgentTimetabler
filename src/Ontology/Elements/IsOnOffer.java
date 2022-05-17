package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

public class IsOnOffer implements Predicate
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
