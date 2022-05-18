package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

import java.util.ArrayList;

public class IsAssignedTo implements Predicate
{
    @Slot(mandatory = true)
    private AID attendingStudentAID;
    
    @Slot(mandatory = true)
    @AggregateSlot(cardMin = 1)
    private ArrayList<TutorialSlot> tutorialSlots;
    
    public AID getAttendingStudentAID() {
        return attendingStudentAID;
    }
    
    public void setAttendingStudentAID(AID attendingStudentAID) {
        this.attendingStudentAID = attendingStudentAID;
    }
    
    public ArrayList<TutorialSlot> getTutorialSlots() {
        return tutorialSlots;
    }
    
    public void setTutorialSlots(ArrayList<TutorialSlot> tutorialSlots) {
        this.tutorialSlots = tutorialSlots;
    }
    
    public IsAssignedTo(AID attendingStudentAID, ArrayList<TutorialSlot> tutorialSlots) {
        this.attendingStudentAID = attendingStudentAID;
        this.tutorialSlots = tutorialSlots;
    }
}
