package Ontology.Elements.Concepts;

import jade.content.Concept;

public enum Preference implements Concept
{
    CANNOT(-3),
    PREFER_NOT(-1),
    NO_PREFERENCE(0),
    PREFER(+1);
    
    public int getUtility() {
        return utility;
    }
    
    private final int utility;
    
    private Preference(int utility) {
        this.utility = utility;
    }
    
}
