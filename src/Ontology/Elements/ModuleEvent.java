package Ontology.Elements;

import jade.content.onto.annotations.Slot;

public class ModuleEvent extends Event
{
    
    @Slot(mandatory = true)
    protected String moduleId;
    
    @Slot(mandatory = true)
    protected int eventId;
    
    public ModuleEvent(String moduleId) {
        super();
        this.moduleId = moduleId;
    }
    @Slot(mandatory = true)
    public String getEventId() {
        return moduleId+"-T-"+Integer.toString(eventId);
    }
    
    @Slot(mandatory = true)
    public String getModuleId() {
        return moduleId;
    }
    
    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }
}
