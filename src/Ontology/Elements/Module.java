package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;

public class Module implements Concept
{
    @Slot(mandatory = true)
    private String moduleId;
    
    private ArrayList<Integer> enrolledStudentIds;
    
    private int tutorialGroupAmount;
    
    private ArrayList<Tutorial> tutorials;
    
    public ArrayList<Integer> getEnrolledStudentIds()
    {
        return enrolledStudentIds;
    }
    
    public void setEnrolledStudentIds(ArrayList<Integer> enrolledStudentIds)
    {
        this.enrolledStudentIds = enrolledStudentIds;
    }
    
    public void addEnrolledStudentId(Integer enrolledStudentId)
    {
        if (this.enrolledStudentIds==null){
            this.enrolledStudentIds=new ArrayList<Integer>();
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
