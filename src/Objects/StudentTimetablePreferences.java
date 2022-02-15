package Objects;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

//THIS CAN BE HASHMAP BECAUSE IT'S NOT BEING SENT
//this actually doesn't need to be in the ontology at all, the student agent just uses it internally to come up with some sort of utility gain/loss to make decisions
public class StudentTimetablePreferences
{
    private ConcurrentHashMap<Long, Preference> timetable;
    
    public StudentTimetablePreferences() {
        var timetable = new ConcurrentHashMap<Long, Preference>();
        for (int i = 1; i <= 45; i++) {
            timetable.put((long) i, Preference.NO_PREFERENCE);
        }
        this.timetable=timetable;
        //timeslots represented as ints to simplify lookup, mon 1-9, tue 10-19 etc
    }
    
    public void setPreference(int i, Preference preference) {
        this.timetable.put((long) i, preference);
    }
    
    public int getTimeslotUtility(Long timeslotId) {
        return timetable.get(timeslotId).getUtility();
    }
    
    public int getTotalUtility(ArrayList<Long> tutorialSlots, StudentTimetablePreferences preferences) {
        var totalUtility = 0;
        
        for (int i = 0; i < tutorialSlots.size(); i++) {
            totalUtility += preferences.getTimeslotUtility(tutorialSlots.get(i));
        }
        
        return totalUtility;
    }
}
