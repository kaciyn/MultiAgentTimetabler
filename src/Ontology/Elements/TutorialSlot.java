package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class TutorialSlot implements Concept
{
    public TutorialSlot() {
    
    }
    
    public TutorialSlot(String moduleId, Long timeslotId) {
        this.moduleId = moduleId;
        this.timeslotId = timeslotId;
    }
    
    @Slot(mandatory = true)
    private String moduleId;
    
    @Slot(mandatory = true)
    private Long timeslotId;
    
    public String getModuleId() {
        return moduleId;
    }
    
    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }
    
    public Long getTimeslotId() {
        return timeslotId;
    }
    
    public void setTimeslotId(Long timeslotId) {
        this.timeslotId = timeslotId;
    }
    
}
