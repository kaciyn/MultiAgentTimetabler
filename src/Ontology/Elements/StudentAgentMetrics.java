package Ontology.Elements;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class StudentAgentMetrics implements Concept
{
    public Long getCurrentTimetableUtility() {
        return currentTimetableUtility;
    }
    
    public void setCurrentTimetableUtility(Long currentTimetableUtility) {
        this.currentTimetableUtility = currentTimetableUtility;
    }
    
    public Long getInitialTimetableUtility() {
        return initialTimetableUtility;
    }
    
    public void setInitialTimetableUtility(Long initialTimetableUtility) {
        this.initialTimetableUtility = initialTimetableUtility;
    }
    
    private Long initialTimetableUtility;
    
    public boolean isTimetableValid() {
        return timetableIsValid;
    }
    
    public void setTimetableIsValid(boolean timetableIsValid) {
        this.timetableIsValid = timetableIsValid;
    }
    
    private boolean timetableIsValid;
    
    @Slot(mandatory = true)
    private Long currentTimetableUtility;
    
    public Long getMessagesSent() {
        return messagesSent;
    }
    
    public void setMessagesSent(Long messagesSent) {
        this.messagesSent = messagesSent;
    }
    
    @Slot(mandatory = true)
    private Long messagesSent;
    
    public boolean isInitialMetrics() {
        return initialMetrics;
    }
    
    public void setInitialMetrics(boolean initialMetrics) {
        this.initialMetrics = initialMetrics;
    }
    
    private boolean initialMetrics;
    
    public boolean isFinalMetrics() {
        return finalMetrics;
    }
    
    public void setFinalMetrics(boolean finalMetrics) {
        this.finalMetrics = finalMetrics;
    }
    
    private boolean finalMetrics;
    
  
    public int getOptimalTimetableUtility() {
        return optimalTimetableUtility;
    }
    
    public void setMaximumTimetableUtility(int optimalTimetableUtility) {
        this.optimalTimetableUtility = optimalTimetableUtility;
    }
    
    private int optimalTimetableUtility;
    
    private float percentageOfOptimum;
    
    public float getPercentageOfOptimum() {
        this.setPercentageOfOptimum();
        return this.getPercentageOfOptimum();
    }
    
    public void setPercentageOfOptimum() {
        if (this.currentTimetableUtility != 0 && this.optimalTimetableUtility != 0) {
            this.percentageOfOptimum = (float) this.currentTimetableUtility / (float) this.optimalTimetableUtility;
        }
    }
}
