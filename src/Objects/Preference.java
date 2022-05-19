package Objects;

import jade.content.onto.annotations.Slot;

public enum Preference
{
    //arbitrarily but significantly lower utility than everything else due to the hard 'cannot' constraint
//utilities are all above 0 to prevent div by 0 issues in calculating optimal utility
    CANNOT(0,"Cannot Attend"),
    PREFER_NOT(1,"Prefer Not To Attend"),
    NO_PREFERENCE(3,"No Preference"),
    PREFER(10,"Prefer To Attend");
    
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
