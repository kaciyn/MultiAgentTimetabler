package Ontology.Elements.Concepts;

import jade.content.Concept;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;

public class Timetable implements Concept
{
    private ArrayList<Timeslot> timetable;
    
    public ArrayList<Timeslot> getTimetable() {
        return timetable;
    }
    
    public void setTimetable(ArrayList<Timeslot> timetable) {
        this.timetable = timetable;
    }
}