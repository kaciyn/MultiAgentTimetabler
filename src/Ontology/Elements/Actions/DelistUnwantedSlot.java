package Ontology.Elements.Actions;

import Ontology.Elements.Concepts.UnwantedTimeslotListing;
import jade.content.AgentAction;
//making a concept for an id seems like overkill?
public class DelistUnwantedSlot implements AgentAction
{
    public UnwantedTimeslotListing getSlotToDelist() {
        return slotToDelist;
    }
    
    public void setSlotToDelist(UnwantedTimeslotListing slotToDelist) {
        this.slotToDelist = slotToDelist;
    }
    
    private UnwantedTimeslotListing slotToDelist;
    
}
