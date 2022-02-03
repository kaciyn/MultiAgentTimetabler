package Ontology.Elements;

import jade.content.Predicate;

public class IsNoLongerOnOffer implements Predicate
{
    //INSTEAD OF SENDING THROUGH ENTIRE LIST OF UNWANTED TUTORIALS JUST SEND SINGLE TUTORIAL
//    private HashMap<Integer, Tutorial> unwantedTutorials;

//    public HashMap<Integer, Tutorial> getUnwantedTutorials() {
//        return unwantedTutorials;
//    }
//    public void setUnwantedTutorials(HashMap<Integer, Tutorial> unwantedTutorials) {
//        this.unwantedTutorials = unwantedTutorials;
//    }
    
    public Tutorial getUnavailableTutorial() {
        return unavailableTutorial;
    }
    
    public void setUnavailableTutorial(Tutorial unavailableTutorial) {
        this.unavailableTutorial = unavailableTutorial;
    }
    
    public Integer getUnavailableTutorialId() {
        return unavailableTutorialId;
    }
    
    public void setUnavailableTutorialId(Integer unavailableTutorialId) {
        this.unavailableTutorialId = unavailableTutorialId;
    }
    
    private Tutorial unavailableTutorial;
    
    private Integer unavailableTutorialId;
    
    
}
