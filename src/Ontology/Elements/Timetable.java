package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.util.concurrent.ConcurrentHashMap;

public class Timetable implements Concept
{
    @Slot(mandatory = true)
    private ConcurrentHashMap<Integer, Event> timetable;
    
    public Timetable() {
        this.timetable = new ConcurrentHashMap<Integer, Event>();
        for (int i = 1; i < 45; i++) {
            timetable.put(i, null);
        }
        //timeslots represented as ints to simplify lookup, mon 1-9, tue 10-19 etc
    }
    
    public void set(int timeslotId, Event event) {
        this.set(timeslotId, event);
    }
    
}
    
