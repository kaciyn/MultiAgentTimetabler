

public class UtilityCounter
{
    private Integer totalSystemUtility;
    private Integer averageSystemUtility;
    
    public void averageUtilityTimes(long startTime, long currentTime) {
        //todo somewhere trigger at say 0,0.25,.5,.75, .9, .95, .99, and 1 - maybe an agent
    }
    
    public void recordAverageUtilityTimesReached(long startTime, long currentTime) {
        
        System.out.println("Average system utility of: " + averageSystemUtility + " reached at: " + (currentTime - startTime));
        
    }
    
    

//    public Integer getAverageSystemUtility(ArrayList<StudentTimetablePreferences> studentsTimetablePreferences) {
//        averageSystemUtility = getTotalSystemUtility(studentsTimetablePreferences) / studentsTimetablePreferences.size();
//        return averageSystemUtility;
//
//    }
//
//    public Integer getTotalSystemUtility(ArrayList<StudentTimetablePreferences> studentsTimetablePreferences) {
//        totalSystemUtility = 0;
//
//        for (StudentTimetablePreferences studentTimetablePreferences : studentsTimetablePreferences) {
//            totalSystemUtility += studentTimetablePreferences.getTotalUtility();
//
//        }
//        return totalSystemUtility;
//    }
//
}
