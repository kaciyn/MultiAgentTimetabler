package Objects;

import Models.Module;
import Ontology.Elements.TutorialSlot;

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
        this.timetable = timetable;
        //timeslots represented as ints to simplify lookup, mon 1-9, tue 10-19 etc
    }
    
    public Preference getPreference(Long slotId) {
        return timetable.get(slotId);
    }
    
    public void setPreference(int i, Preference preference) {
        this.timetable.put((long) i, preference);
    }
    
    public int getTimeslotUtility(Long timeslotId) {
        return timetable.get(timeslotId).getUtility();
    }
    
    public int getTotalUtility(ArrayList<TutorialSlot> tutorialSlots, StudentTimetablePreferences preferences) {
        var totalUtility = 0;
        
        for (int i = 0; i < tutorialSlots.size(); i++) {
            var tutorialTimeSlot = tutorialSlots.get(i).getTimeslotId();
            
            totalUtility += preferences.getTimeslotUtility(tutorialTimeSlot);
        }
        
        return totalUtility;
    }
    
    public ArrayList<TutorialSlot> getPerfectTutorialAssignments(ArrayList<Module> modules, StudentTimetablePreferences preferences) {
        var optimalTutorialAssignments = new ArrayList<TutorialSlot>();
        for (int i = 0; i < modules.size(); i++) {
            
            var tutorials = modules.get(i).getTutorials();
            
            var bestTutorial = tutorials.get(0);
            var bestTutorialUtility = preferences.getTimeslotUtility(bestTutorial.getTimeslotId());
            
            for (int j = 1; j < tutorials.size(); j++) {
                var tutorial = tutorials.get(j);
                var util = preferences.getTimeslotUtility(tutorial.getTimeslotId());
                
                if (util > bestTutorialUtility) {
                    bestTutorialUtility = util;
                    bestTutorial = tutorial;
                }
            }
            
            optimalTutorialAssignments.add(new TutorialSlot(modules.get(i).getModuleId(), bestTutorial.getTimeslotId()));
            
        }
        
        return optimalTutorialAssignments;
    }
    
    //best utility achievable given constraints of schedule and own preferences
    public int getOptimalUtility(ArrayList<Module> modules, StudentTimetablePreferences preferences) {
        var totalOptimalUtility = 0;
        for (int i = 0; i < modules.size(); i++) {
            
            var tutorials = modules.get(i).getTutorials();
            
            var bestTutorial = tutorials.get(0);
            var bestTutorialUtility = preferences.getTimeslotUtility(bestTutorial.getTimeslotId());
            
            for (int j = 1; j < tutorials.size(); j++) {
                var tutorial = tutorials.get(j);
                var util = preferences.getTimeslotUtility(tutorial.getTimeslotId());
                
                if (util > bestTutorialUtility) {
                    bestTutorialUtility = util;
                    bestTutorial = tutorial;
                }
            }
            
            totalOptimalUtility += bestTutorialUtility;
            
        }
        
        return totalOptimalUtility;
    }
    
    public boolean isValid(ArrayList<TutorialSlot> tutorialSlots, StudentTimetablePreferences preferences) {
        for (TutorialSlot tutorialSlot : tutorialSlots) {
            var preference = preferences.getPreference(tutorialSlot.getTimeslotId());
            if (preference.equals(Preference.CANNOT)) {
                return false;
            }
            
        }
        return true;
    }
    
}
