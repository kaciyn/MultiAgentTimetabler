package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class IsListed implements Predicate
{
    
    @Slot(mandatory = true)
    private UnwantedTimeslotListing unwantedTimeslotListing;
    
    public UnwantedTimeslotListing getUnwantedTimeslotListing() {
        return unwantedTimeslotListing;
    }
    
    public void setUnwantedTimeslotListing(UnwantedTimeslotListing unwantedTimeslotListing) {
        this.unwantedTimeslotListing = unwantedTimeslotListing;
    }
    
    
    private AID requestingStudent;
    
    public AID getRequestingStudent() {
        return requestingStudent;
    }
    
    public void setRequestingStudent(AID requestingStudent) {
        this.requestingStudent = requestingStudent;
    }
}
