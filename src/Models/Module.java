package Models;

import java.util.ArrayList;

public class Module
{
    private String moduleId;
    
    private ArrayList<Long> enrolledStudentIds;
    
    private int tutorialGroupAmount;
    
    private ArrayList<Tutorial> tutorials;
    
    public ArrayList<Long> getEnrolledStudentIds()
    {
        return enrolledStudentIds;
    }
    
    public void setEnrolledStudentIds(ArrayList<Long> enrolledStudentIds)
    {
        this.enrolledStudentIds = enrolledStudentIds;
    }
    
    public void addEnrolledStudentId(Long enrolledStudentId)
    {
        if (this.enrolledStudentIds==null){
            this.enrolledStudentIds=new ArrayList<Long>();
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
