package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public enum Preference implements Concept
{
    CANNOT(-3,"Cannot Attend"),
    PREFER_NOT(-1,"Prefer Not To Attend"),
    NO_PREFERENCE(0,"No Preference"),
    PREFER(+1,"Prefer To Attend");
    
    @Slot(mandatory = true)
    private final int utility;
    
   
    @Slot(mandatory = true)
    private final String description;
    
    Preference(int utility, String description) {
        this.utility = utility;
        this.description = description;
    }
    
    @Slot(mandatory = true)
    public int getUtility() {
        return utility;
    }
    
    @Slot(mandatory = true)
        public String getDescription() {
        return description;
    }
    
}
