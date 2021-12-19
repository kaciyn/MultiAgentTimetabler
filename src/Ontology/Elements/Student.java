package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;

public class Student implements Concept
{
    @Slot(mandatory = true)
    public int matriculationNumber;
    
    private ArrayList<String> moduleIds;
    
    private StudentTimetablePreferences studentTimetablePreferences;
    
    //possibly i could rejig this to have moduletutorials and then have a min/max of 1 for each moduletutorial but also, i am tired
    //would have Loved to have done the max/min by moduleIds.size() but jade can't let me have nice things (non-constant values)
    @AggregateSlot(cardMax = 3)
    private ArrayList<Tutorial> tutorials;
    
    @Slot(mandatory = true)
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
    
    public void addTutorial(Tutorial tutorial) {
        if (this.tutorials == null) {
            this.tutorials = new ArrayList<Tutorial>();
        }
        this.tutorials.add(tutorial);
    }
    
    public void removeTutorial(Tutorial tutorial) {
        this.tutorials.remove(tutorial);
    }
    
}
