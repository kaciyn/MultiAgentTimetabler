package Ontology.Elements;

import jade.content.Predicate;

import java.util.HashMap;

public class AreOnOffer implements Predicate
{
    
    private HashMap<Integer, Tutorial> unwantedTutorials;
    public HashMap<Integer, Tutorial> getUnwantedTutorials() {
        return unwantedTutorials;
    }
    
    public void setUnwantedTutorials(HashMap<Integer, Tutorial> unwantedTutorials) {
        this.unwantedTutorials = unwantedTutorials;
    }
    
    
    
}
