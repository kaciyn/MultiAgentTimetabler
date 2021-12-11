package Ontology.Elements;

import jade.content.Concept;

import java.util.concurrent.ConcurrentHashMap;
//i'd want to extend this from timetable but in terms of the ontology i don't think it makes sense?
//a preference is not a type of event
public class StudentTimetablePreferences implements Concept
{
    private ConcurrentHashMap<Integer, Preference> timetable;
    
    public StudentTimetablePreferences() {
        var timetable = new ConcurrentHashMap<Integer, Preference>();
        for (int i = 1; i < 45; i++) {
            timetable.put(i, null);
        }
        //timeslots represented as ints to simplify lookup, mon 1-9, tue 10-19 etc
    }
    
    public void set(int i, Preference preference) {
        this.set(i, preference);
    }
    
    public Integer getTotalUtility(){
        var totalUtility=0;
        for (int i = 1; i < 45; i++) {
            totalUtility+=timetable.get(i).getUtility();
        }
        return totalUtility;
    }
    
    public int getTimeslotUtility(Integer timeslotId){
       return timetable.get(timeslotId).getUtility();
    }
}
