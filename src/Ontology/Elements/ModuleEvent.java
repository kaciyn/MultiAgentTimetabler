package Ontology.Elements;

import jade.content.onto.annotations.Slot;

public class ModuleEvent extends Event
{
    protected String moduleId;
    
    protected int eventId;
    
    @Slot(mandatory = true)
    public String getEventId() {
        return moduleId + "-T-" + timeslotId;
    }
    
    @Slot(mandatory = true)
    public String getModuleId() {
        return moduleId;
    }
    
    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }
}
