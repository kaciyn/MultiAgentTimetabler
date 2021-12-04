package Ontology.Elements;

import jade.content.Predicate;

import java.util.ArrayList;

public class IsAssignedTo implements Predicate
{
    private Student attendingStudent;
    private ArrayList<Tutorial> tutorial;
    
    public Student getAttendingStudent() {
        return attendingStudent;
    }
    
    public void setAttendingStudent(Student attendingStudent) {
        this.attendingStudent = attendingStudent;
    }
    
    public ArrayList<Tutorial> getTutorials() {
        return tutorial;
    }
    
    public void setTutorials(ArrayList<Tutorial> tutorials) {
        this.tutorial = tutorial;
    }
    
    public void swapTutorials(Tutorial originalTutorial, Tutorial newTutorial) {
        this.tutorial.remove(originalTutorial);
        this.tutorial.add(newTutorial);
        
    }
}
