package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;

public class Module implements Concept
{
    @Slot(mandatory = true)
    private String moduleId;
    
    private ArrayList<java.lang.Integer> enrolledStudentIds;
    
    private int tutorialGroupAmount;
    
    private ArrayList<Tutorial> tutorials;
    
    public ArrayList<java.lang.Integer> getEnrolledStudentIds()
    {
        return enrolledStudentIds;
    }
    
    public void setEnrolledStudentIds(ArrayList<java.lang.Integer> enrolledStudentIds)
    {
        this.enrolledStudentIds = enrolledStudentIds;
    }
    
    public void addEnrolledStudentId(java.lang.Integer enrolledStudentId)
    {
        if (this.enrolledStudentIds==null){
            this.enrolledStudentIds=new ArrayList<java.lang.Integer>();
        }
        this.enrolledStudentIds.add(enrolledStudentId);
    }
    
    public ArrayList<Tutorial> getTutorials()
    {
        return tutorials;
    }
    
    public void setTutorials(ArrayList<Tutorial> tutorials)
    {
        this.tutorials = tutorials;
    }
    
    @Slot(mandatory = true)
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
