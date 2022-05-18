package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
//unsure if this and is onoffer should be merged with a bool but oh well
public class IsDelisted implements Predicate
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
