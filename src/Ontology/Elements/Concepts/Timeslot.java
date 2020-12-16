package Ontology.Elements.Concepts;

import jade.content.Concept;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

public class Timeslot implements Concept
{
    private DayOfWeek day;
    //can be from 9.00 - 17.00
    private LocalTime startTime;
    //can only be 1 in this system
    private Duration duration;
    
    
    public DayOfWeek getDay() {
        return day;
    }
    
    public void setDay(DayOfWeek day) {
        this.day = day;
    }
    
    public LocalTime getStartTime()
    {
        return startTime;
    }
    
    public void setStartTime(LocalTime startTime)
    {
        this.startTime = startTime;
    }
    
    public Duration getDuration()
    {
        return duration;
    }
    
    public void setDuration(Duration duration)
    {
        this.duration = duration;
    }
}
