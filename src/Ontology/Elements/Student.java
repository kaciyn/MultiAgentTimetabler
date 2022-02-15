package Ontology.Elements;

import Objects.StudentTimetablePreferences;
import jade.content.Concept;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;

public class Student implements Concept
{
    private int matriculationNumber;
    
    private ArrayList<String> moduleIds;
    
    //possibly i could rejig this to have moduletutorials and then have a min/max of 1 for each moduletutorial but also, i am tired
    //would have Loved to have done the max/min by moduleIds.size() but jade can't let me have nice things (non-constant values)
    private ArrayList<Long> tutorialSlots;
    
    private StudentTimetablePreferences studentTimetablePreferences;
    
    
    @Slot(mandatory = true)
    public int getMatriculationNumber() {
        return matriculationNumber;
    }
    
    public void setMatriculationNumber(int matriculationNumber) {
        this.matriculationNumber = matriculationNumber;
    }
    
    @Slot(mandatory = true)
    @AggregateSlot(cardMin = 1, cardMax = 3)
    public ArrayList<String> getModuleIds() {
        return moduleIds;
    }
    
    public void setModuleIds(ArrayList<String> moduleIds) {
        this.moduleIds = moduleIds;
    }
    
    @Slot(mandatory = true)
    public StudentTimetablePreferences getStudentTimetablePreferences() {
        return studentTimetablePreferences;
    }
    
    public void setStudentTimetablePreferences(StudentTimetablePreferences studentTimetablePreferences) {
        this.studentTimetablePreferences = studentTimetablePreferences;
    }
    
    @AggregateSlot(cardMin = 1, cardMax = 3)
    public ArrayList<Long> getTutorialSlots() {
        if (tutorialSlots == null) {
            tutorialSlots = new ArrayList<Long>();
        }
        return tutorialSlots;
    }
    
    public void setTutorialSlots(ArrayList<Long> tutorialSlots) {
        this.tutorialSlots = tutorialSlots;
    }
    
    public void addTutorialSlot(Long tutorial) {
        if (this.tutorialSlots == null) {
            this.tutorialSlots = new ArrayList<Long>();
        }
        this.tutorialSlots.add(tutorial);
    }
    
    public void removeTutorialSlot(Long tutorial) {
        if (this.tutorialSlots.contains(tutorial)) {
            this.tutorialSlots.remove(tutorial);
        }
    }
    
}
