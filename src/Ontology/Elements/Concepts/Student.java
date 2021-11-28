package Ontology.Elements.Concepts;

import jade.content.Concept;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Student implements Concept
{
    private int matriculationNumber;
    
    private ArrayList<Module> modules;
    
    private StudentTimetablePreferences studentTimetablePreferences;
    
    private ArrayList<Integer> tutorialAssignments;
    
    public Student(int matriculationNumber, ArrayList<Module> modules, StudentTimetablePreferences studentTimetablePreferences, ArrayList<Integer> tutorialAssignments) {
        this.matriculationNumber = matriculationNumber;
        this.modules = modules;
        this.studentTimetablePreferences = studentTimetablePreferences;
        this.tutorialAssignments = tutorialAssignments;
    }
    
    public Student(int matriculationNumber, StudentTimetablePreferences studentTimetablePreferences) {
        this.matriculationNumber = matriculationNumber;
        this.studentTimetablePreferences = studentTimetablePreferences;
    }
    
    public int getMatriculationNumber() {
        return matriculationNumber;
    }
    
    public void setMatriculationNumber(int matriculationNumber) {
        this.matriculationNumber = matriculationNumber;
    }
    
    public ArrayList<Module> getModules() {
        return modules;
    }
    
    public void setModules(ArrayList<Module> modules) {
        this.modules = modules;
    }
    
    public StudentTimetablePreferences getStudentTimetablePreferences() {
        return studentTimetablePreferences;
    }
    
    public void setStudentTimetablePreferences(StudentTimetablePreferences studentTimetablePreferences) {
        this.studentTimetablePreferences = studentTimetablePreferences;
    }
    
    public ArrayList<Integer> getTutorialAssignments() {
        return tutorialAssignments;
    }
    
    public void setTutorialAssignments(ArrayList<Integer> tutorialAssignments) {
        this.tutorialAssignments = tutorialAssignments;
    }
    

}
