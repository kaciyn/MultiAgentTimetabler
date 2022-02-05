package Ontology.Elements;

import jade.content.Predicate;

public class IsOnOffer implements Predicate
{
    //INSTEAD OF SENDING THROUGH ENTIRE LIST OF UNWANTED TUTORIALS JUST SEND SINGLE TUTORIAL, SENDING THROUGH TUTORIALS THAT AREN'T ON OFFER ANYMORE
//    private HashMap<Integer, Tutorial> unwantedTutorial;
//
//    public HashMap<Integer, Tutorial> getUnwantedTutorial() {
//        return unwantedTutorial;
//    }
//    public void setUnwantedTutorial(HashMap<Integer, Tutorial> unwantedTutorial) {
//        this.unwantedTutorial = unwantedTutorial;
//    }
    
    public Integer getUnwantedTutorialSlot() {
        return unwantedTutorialSlot;
    }
    
    public void setUnwantedTutorialSlot(Integer unwantedTutorialSlot) {
        this.unwantedTutorialSlot = unwantedTutorialSlot;
    }
    
    public Integer getUnwantedTutorialId() {
        return unwantedIntegerId;
    }
    
    public void setUnwantedTutorialId(Integer unwantedIntegerId) {
        this.unwantedIntegerId = unwantedIntegerId;
    }
    
    private Integer unwantedTutorialSlot;
    
    private Integer unwantedIntegerId;
    
    
    
}
