package Ontology.Elements.Concepts;

import jade.content.Concept;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class Timetable implements Concept
{
    //timeslots represented as ints to simplify lookup, mon 1-9, tue 10-19 etc
    private int[] timeslotIDs = IntStream.range(1,45).toArray();
    
    private ConcurrentHashMap<Integer,Tutorial> timetable;
    
    public ArrayList<Timeslot> getTimetable() {
        return timetable;
    }
    
    public void setTimetable(ArrayList<Timeslot> timetable) {
        this.timetable = timetable;
    }
    
}
    
