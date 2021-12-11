package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

import java.util.ArrayList;

public class IsUnwanted implements Predicate
{
    private AID studentAID;
    public ArrayList<Tutorial> tutorials;
    
    @Slot(mandatory = true)
    
    public AID getStudentAID() {
        return studentAID;
    }
    
    @Slot(mandatory = true)
    public void setStudentAID(AID studentAID) {
        this.studentAID = studentAID;
    }
    
    @Slot(mandatory = true)
    public ArrayList<Tutorial> getTutorials() {
        return tutorials;
    }
    
    @Slot(mandatory = true)
    public void setTutorials(ArrayList<Tutorial> tutorials) {
        this.tutorials = tutorials;
    }
    
    @Slot(mandatory = true)
    public void addTutorial(Tutorial tutorial) {
        this.tutorials.add(tutorial);
    }
    
}