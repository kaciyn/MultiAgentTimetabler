package Ontology.Elements;

import jade.content.Predicate;

import java.util.HashMap;

public class IsOnOffer implements Predicate
{
    //INSTEAD OF SENDING THROUGH ENTIRE LIST OF UNWANTED TUTORIALS JUST SEND SINGLE TUTORIAL, SENDING THROUGH TUTORIALS THAT AREN'T ON OFFER ANYMORE
    private HashMap<Integer, Tutorial> unwantedTutorial;
    
    public HashMap<Integer, Tutorial> getUnwantedTutorial() {
        return unwantedTutorial;
    }
    public void setUnwantedTutorial(HashMap<Integer, Tutorial> unwantedTutorial) {
        this.unwantedTutorial = unwantedTutorial;
    }
    
    
    private Tutorial unwantedTutorial;
    
    private Integer  unwantedTutorialId;
    
    
    
}
