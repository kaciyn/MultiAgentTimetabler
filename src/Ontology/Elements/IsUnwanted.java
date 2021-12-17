package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class IsUnwanted implements Predicate
{
    private AID studentAID;
    
    public Tutorial getTutorial() {
        return tutorial;
    }
    
    public void setTutorial(Tutorial tutorial) {
        this.tutorial = tutorial;
    }
    
    //    public ArrayList<Tutorial> tutorials;
    private Tutorial tutorial;
    
    @Slot(mandatory = true)
    
    public AID getStudentAID() {
        return studentAID;
    }
    
    @Slot(mandatory = true)
    public void setStudentAID(AID studentAID) {
        this.studentAID = studentAID;
    }
    
}
