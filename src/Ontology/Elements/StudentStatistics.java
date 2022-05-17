package Ontology.Elements;

import jade.content.onto.annotations.Slot;

public class StudentStatistics
{
    public Long getCurrentTotalUtility() {
        return currentTotalUtility;
    }
    
    public void setCurrentTotalUtility(Long currentTotalUtility) {
        this.currentTotalUtility = currentTotalUtility;
    }
    
    @Slot(mandatory = true)
    private Long currentTotalUtility;
    
    public Long getMessagesSent() {
        return messagesSent;
    }
    
    public void setMessagesSent(Long messagesSent) {
        this.messagesSent = messagesSent;
    }
    
    @Slot(mandatory = true)
    private Long messagesSent;
    
    public boolean isInitialStats() {
        return isInitialStats;
    }
    
    public void setInitialStats(boolean initialStats) {
        isInitialStats = initialStats;
    }
    
    public boolean isInitialStats;
    
    public boolean isFinalStats() {
        return finalStats;
    }
    
    public void setFinalStats(boolean finalStats) {
        this.finalStats = finalStats;
    }
    
    private boolean finalStats;
    
    
}
