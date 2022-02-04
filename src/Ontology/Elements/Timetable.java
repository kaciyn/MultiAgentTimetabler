package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Timetable implements Concept
{
//    private ConcurrentHashMap<Integer, Event> timetable;
    
    public Timetable(Event[] timetable) {
        this.timetable = new Event[44];
    }
   
    @Slot(mandatory = true)
    private Event[] timetable;

    public Event[] getTimetable() {
        return timetable;
    }
    
    public void setTimetable(Event[] timetable) {
        this.timetable = timetable;
    }
    
    
    public void setEvent(int timeslotId, Event event) {
        this.timetable[timeslotId-1]=event;
    }
    
}
    
