package Ontology.Elements;

import jade.content.Predicate;

import java.util.ArrayList;

public class IsAssignedTo implements Predicate
{
    private Student attendingStudent;
    private ArrayList<Tutorial> tutorials;
    
    public Student getAttendingStudent() {
        return attendingStudent;
    }
    
    public void setAttendingStudent(Student attendingStudent) {
        this.attendingStudent = attendingStudent;
    }
    
    public ArrayList<Tutorial> getTutorials() {
        return tutorials;
    }

    public void setTutorials(ArrayList<Tutorial> tutorials) {
        this.tutorials = tutorials;
    }
    
}
