package Ontology.Elements;

import jade.content.Predicate;

public class IsOnOffer implements Predicate
{
    //INSTEAD OF SENDING THROUGH ENTIRE LIST OF UNWANTED TUTORIALS JUST SEND SINGLE TUTORIAL, SENDING THROUGH TUTORIALS THAT AREN'T ON OFFER ANYMORE
//    private HashMap<Long, Tutorial> unwantedTutorial;
//
//    public HashMap<Long, Tutorial> getUnwantedTutorial() {
//        return unwantedTutorial;
//    }
//    public void setUnwantedTutorial(HashMap<Long, Tutorial> unwantedTutorial) {
//        this.unwantedTutorial = unwantedTutorial;
//    }
    
    public Long getUnwantedTutorialSlot() {
        return unwantedTutorialSlot;
    }
    
    public void setUnwantedTutorialSlot(Long unwantedTutorialSlot) {
        this.unwantedTutorialSlot = unwantedTutorialSlot;
    }
    
    public Long getUnwantedTutorialId() {
        return unwantedTutorialId;
    }
    
    public void setUnwantedTutorialId(Long unwantedLongId) {
        this.unwantedTutorialId = unwantedLongId;
    }
    
    private Long unwantedTutorialSlot;
    
    private Long unwantedTutorialId;
    
    
    
}
