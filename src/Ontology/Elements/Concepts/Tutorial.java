package Ontology.Elements.Concepts;

public class Tutorial extends Event
{
    private int moduleId;
    
    private int timeSlotId;

    public int getModuleId()
    {
        return moduleId;
    }

    public void setModuleId(int moduleId)
    {
        this.moduleId = moduleId;
    }
    
    public int getTimeSlotId() {
        return timeSlotId;
    }
    
    public void setTimeSlotId(int timeSlotId) {
        this.timeSlotId = timeSlotId;
    }
    
}
