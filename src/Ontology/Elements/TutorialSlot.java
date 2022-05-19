package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class TutorialSlot implements Concept
{
    public TutorialSlot() {
    
    }
    
    public TutorialSlot(String moduleId, int timeslotId) {
        this.moduleId = moduleId;
        this.timeslotId = timeslotId;
    }
    
    @Slot(mandatory = true)
    private String moduleId;
    
    @Slot(mandatory = true)
    private int timeslotId;
    
    public String getModuleId() {
        return moduleId;
    }
    
    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }
    
    public int getTimeslotId() {
        return timeslotId;
    }
    
    public void setTimeslotId(int timeslotId) {
        this.timeslotId = timeslotId;
    }
    
}
