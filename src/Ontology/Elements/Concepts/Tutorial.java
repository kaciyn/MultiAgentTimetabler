package Ontology.Elements.Concepts;

import java.util.ArrayList;

public class Tutorial extends Event
{
    private int moduleId;
    
    private int timeSlotId;
    
    private ArrayList<Tutorial> tutorials;
    
    public Tutorial(int moduleId, ArrayList<Tutorial> tutorials) {
        this.moduleId = moduleId;
        this.tutorials = tutorials;
    }
    
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
