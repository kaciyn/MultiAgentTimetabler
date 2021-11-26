package Ontology.Elements.Concepts;

import jade.content.Concept;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Timetable implements Concept
{
    
    public Timetable() {
        var timetable = new ConcurrentHashMap<Integer, ArrayList<Tutorial>>();
        for (int i = 1; i < 45; i++) {
            timetable.put(i, new ArrayList<Tutorial>());
        }
        //timeslots represented as ints to simplify lookup, mon 1-9, tue 10-19 etc
    }
    
    
    
    
}
    
