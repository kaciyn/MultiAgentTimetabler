package Ontology.Elements;

import jade.content.Predicate;

import java.util.ArrayList;

public class IsAssignedTo implements Predicate
{
    private Student attendingStudent;
    private ArrayList<Integer> tutorialSlots;
    
    public Student getAttendingStudent() {
        return attendingStudent;
    }
    
    public void setAttendingStudent(Student attendingStudent) {
        this.attendingStudent = attendingStudent;
    }
    
    public ArrayList<Integer> getTutorialSlots() {
        return tutorialSlots;
    }

    public void setTutorialSlots(ArrayList<Integer> tutorialSlots) {
        this.tutorialSlots = tutorialSlots;
    }
    
}
