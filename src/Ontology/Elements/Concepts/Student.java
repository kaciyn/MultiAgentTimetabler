package Ontology.Elements.Concepts;

import jade.content.Concept;

import java.util.ArrayList;

public class Student implements Concept
{
    private int matriculationNumber;
    
    private ArrayList<String> moduleIds;
    
    private StudentTimetablePreferences studentTimetablePreferences;
    
    private ArrayList<Tutorial> tutorials;
    
    public Student(int matriculationNumber, ArrayList<String> moduleIds, StudentTimetablePreferences studentTimetablePreferences, ArrayList<Tutorial> tutorialAssignments) {
        this.matriculationNumber = matriculationNumber;
        this.moduleIds = moduleIds;
        this.studentTimetablePreferences = studentTimetablePreferences;
        this.tutorials = tutorialAssignments;
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
    
    public ArrayList<String> getModuleIds() {
        return moduleIds;
    }
    
    public void setModuleIds(ArrayList<String> moduleIds) {
        this.moduleIds = moduleIds;
    }
    
    public StudentTimetablePreferences getStudentTimetablePreferences() {
        return studentTimetablePreferences;
    }
    
    public void setStudentTimetablePreferences(StudentTimetablePreferences studentTimetablePreferences) {
        this.studentTimetablePreferences = studentTimetablePreferences;
    }
    
    public ArrayList<Tutorial> getTutorials() {
        return tutorials;
    }
    
    public void setTutorials(ArrayList<Tutorial> tutorials) {
        this.tutorials = tutorials;
    }
    

}
