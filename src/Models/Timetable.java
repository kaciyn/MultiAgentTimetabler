package Models;

public class Timetable
{
    private Tutorial[] timetable;
    
    public Timetable() {
        this.timetable = new Tutorial[44];
    }
    //this was originally going to be a whole thing but it's overkill
//    @Slot(mandatory = true)
//    @AggregateSlot(cardMin = 44, cardMax = 44)
    public Tutorial[] getTimetable() {
        return timetable;
    }
    
    public void setTimetable(Tutorial[] timetable) {
        this.timetable = timetable;
    }
    
    public void setTutorial(int timeslotId, Tutorial Tutorial) {
        this.timetable[timeslotId - 1] = Tutorial;
    }
    
    
    
}
    
