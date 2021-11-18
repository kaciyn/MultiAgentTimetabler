package Ontology.Elements.Concepts;

import jade.content.Concept;

import java.util.ArrayList;

public class Module implements Concept
{
    private String moduleId;
    private ArrayList<Integer> enrolledStudentIds;
    private int tutorialGroupNumber;
    private ArrayList<Tutorial> tutorials;

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
}
