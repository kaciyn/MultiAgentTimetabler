package Ontology.Elements.Concepts;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;
import java.util.Random;

public class Module implements Concept
{
    @Slot(mandatory = true)
    private String moduleId;
    private ArrayList<Integer> enrolledStudentIds;
    private int tutorialGroupAmount;
    private ArrayList<Tutorial> tutorials;
    
    public Module(String moduleId, int tutorialGroupAmount, ArrayList<Tutorial> tutorials) {
        this.moduleId = moduleId;
        this.tutorialGroupAmount = tutorialGroupAmount;
        this.tutorials = tutorials;
    }
    
    public Module(String moduleId, int tutorialGroupAmount) {
        this.moduleId = moduleId;
        this.tutorialGroupAmount = tutorialGroupAmount;
    }
    
    public Module(String moduleId) {
        this.moduleId = moduleId;
    }
    
    public ArrayList<Integer> getEnrolledStudentIds()
    {
        return enrolledStudentIds;
    }
    
    public void setEnrolledStudentIds(ArrayList<Integer> enrolledStudentIds)
    {
        this.enrolledStudentIds = enrolledStudentIds;
    }
    
    public ArrayList<Tutorial> getTutorials()
    {
        return tutorials;
    }
    
    public void setTutorials(ArrayList<Tutorial> tutorials)
    {
        this.tutorials = tutorials;
    }
    
    public String getModuleId() {
        return moduleId;
    }
    
    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }
    
    public int getTutorialGroupAmount() {
        return tutorialGroupAmount;
    }
    
    public void setTutorialGroupAmount(int tutorialGroupAmount) {
        this.tutorialGroupAmount = tutorialGroupAmount;
    }
}
