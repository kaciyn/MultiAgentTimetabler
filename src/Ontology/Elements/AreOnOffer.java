package Ontology.Elements;

import jade.content.Predicate;

import java.util.ArrayList;

public class AreOnOffer implements Predicate
{
    public ArrayList<Integer> getUnwantedTutorialSlotIds() {
        return unwantedTutorialSlotIds;
    }
    
    public void setUnwantedTutorialSlotIds(ArrayList<Integer> unwantedTutorialSlotIds) {
        this.unwantedTutorialSlotIds = unwantedTutorialSlotIds;
    }
    
    private ArrayList<Integer> unwantedTutorialSlotIds;
    
    
}
