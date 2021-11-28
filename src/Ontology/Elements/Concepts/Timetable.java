package Ontology.Elements.Concepts;

import jade.content.Concept;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Timetable implements Concept
{
    private ConcurrentHashMap<Integer, Object> timetable;
    
    public Timetable() {
        this.timetable = new ConcurrentHashMap<Integer, Object>();
        for (int i = 1; i < 45; i++) {
            timetable.put(i, null);
        }
        //timeslots represented as ints to simplify lookup, mon 1-9, tue 10-19 etc
    }
    
    public void set(int timeslotId, Object object) {
        this.set(timeslotId, object);
    }
    
}
    
