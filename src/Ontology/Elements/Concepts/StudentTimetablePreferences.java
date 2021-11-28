package Ontology.Elements.Concepts;

import java.util.concurrent.ConcurrentHashMap;

public class StudentTimetablePreferences extends Timetable
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
    
    public int getTotalUtility(){
        var totalUtility=0;
        for (int i = 1; i < 45; i++) {
            totalUtility+=timetable.get(i).getUtility();
        }
        return totalUtility;
    }
    
    public int getUtility(int timeslotId){
       return timetable.get(timeslotId).getUtility();
    }
}
