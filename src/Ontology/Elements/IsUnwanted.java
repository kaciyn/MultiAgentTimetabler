package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class IsUnwanted implements Predicate
{
    private AID studentAID;
    private Long tutorialSlot;
    
    public Long getTutorialSlot() {
        return tutorialSlot;
    }
    
    public void setTutorialSlot(Long tutorialSlot) {
        this.tutorialSlot = tutorialSlot;
    }
    
    
    @Slot(mandatory = true)
    public AID getStudentAID() {
        return studentAID;
    }
    
    @Slot(mandatory = true)
    public void setStudentAID(AID studentAID) {
        this.studentAID = studentAID;
    }
    
}
