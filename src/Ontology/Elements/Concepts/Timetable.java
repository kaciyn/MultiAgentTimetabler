package Ontology.Elements.Concepts;

import jade.content.Concept;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;

public class Timetable implements Concept
{
private HashMap<DayOfWeek,Timeslot> timetable;

    public HashMap<DayOfWeek, Timeslot> getTimetable()
    {
        return timetable;
    }

    public void setTimetable(HashMap<DayOfWeek, Timeslot> timetable)
    {
        this.timetable = timetable;
    }
}
