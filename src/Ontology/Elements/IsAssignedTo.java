package Ontology.Elements;

import jade.content.Predicate;

import java.util.ArrayList;

public class IsAssignedTo implements Predicate
{
    private Student attendingStudent;
    private ArrayList<Long> tutorialSlots;
    
    public Student getAttendingStudent() {
        return attendingStudent;
    }
    
    public void setAttendingStudent(Student attendingStudent) {
        this.attendingStudent = attendingStudent;
    }
    
    public ArrayList<Long> getTutorialSlots() {
        return tutorialSlots;
    }

    public void setTutorialSlots(ArrayList<Long> tutorialSlots) {
        this.tutorialSlots = tutorialSlots;
    }
    
}
