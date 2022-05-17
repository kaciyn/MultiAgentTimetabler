package Models;

import Objects.StudentTimetablePreferences;
import Ontology.Elements.TutorialSlot;
import jade.content.onto.annotations.Slot;

import java.util.ArrayList;

public class Student
{
    private Long matriculationNumber;
    
    private ArrayList<String> moduleIds;
    
    private ArrayList<TutorialSlot> tutorialSlots;
    
    private StudentTimetablePreferences studentTimetablePreferences;
    
    @Slot(mandatory = true)
    public Long getMatriculationNumber() {
        return matriculationNumber;
    }
    
    public void setMatriculationNumber(Long matriculationNumber) {
        this.matriculationNumber = matriculationNumber;
    }
    
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
    
    public ArrayList<TutorialSlot> getTutorialSlots() {
        if (tutorialSlots == null) {
            tutorialSlots = new ArrayList<TutorialSlot>();
        }
        return tutorialSlots;
    }
    
    public void setTutorialSlots(ArrayList<TutorialSlot> tutorialSlots) {
        this.tutorialSlots = tutorialSlots;
    }
    
    public void addTutorialSlot(TutorialSlot tutorial) {
        if (this.tutorialSlots == null) {
            this.tutorialSlots = new ArrayList<TutorialSlot>();
        }
        this.tutorialSlots.add(tutorial);
    }
    
    public void removeTutorialSlot(TutorialSlot tutorial) {
        if (this.tutorialSlots.contains(tutorial)) {
            this.tutorialSlots.remove(tutorial);
        }
    }
    
}
