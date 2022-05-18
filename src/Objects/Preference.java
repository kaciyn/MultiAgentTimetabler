package Objects;

import jade.content.onto.annotations.Slot;

public enum Preference
{
    //arbitrarily but significantly lower utility than everything else due to the hard 'cannot' constraint
    //if considering CANNOT a hard constraint, don't set utility affect but instead mark timetable as invalid due to constraint violation
//    CANNOT(-100,"Cannot Attend"),
    CANNOT(0,"Cannot Attend"),
    PREFER_NOT(-1,"Prefer Not To Attend"),
    NO_PREFERENCE(0,"No Preference"),
    PREFER(+5,"Prefer To Attend");
    
    private final int utility;
    
    private final String description;
    
    Preference(int utility, String description) {
        this.utility = utility;
        this.description = description;
    }
    
    @Slot(mandatory = true)
    public int getUtility() {
        return utility;
    }
    
    @Slot(mandatory = true)
        public String getDescription() {
        return description;
    }
    
}
