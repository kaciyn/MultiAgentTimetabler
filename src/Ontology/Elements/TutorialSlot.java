package Ontology.Elements;

import jade.content.onto.annotations.Slot;

public class TutorialSlot extends Tutorial
{
    public TutorialSlot(String moduleId, int capacity) {
        super(moduleId, capacity);
    }
    
    public boolean isOnOfferLock() {
        return isOnOfferLock;
    }
    
    public void setOnOfferLock(boolean onOfferLock) {
        isOnOfferLock = onOfferLock;
    }
    
    @Slot(mandatory = true)
    private boolean isOnOfferLock;
}
