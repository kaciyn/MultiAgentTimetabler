package Ontology.Elements;

import jade.content.Concept;

public class UtilityThresholds implements Concept
{
    private Float utilityThreshold0;
    private Float utilityThreshold1;
    private Float finalUtilityThreshold;
    
    public Float getUtilityThreshold0() {
        return utilityThreshold0;
    }
    
    public void setUtilityThreshold0(Float utilityThreshold0) {
        this.utilityThreshold0 = utilityThreshold0;
    }
    
    public Float getUtilityThreshold1() {
        return utilityThreshold1;
    }
    
    public void setUtilityThreshold1(Float utilityThreshold1) {
        this.utilityThreshold1 = utilityThreshold1;
    }
    
    public Float getFinalUtilityThreshold() {
        return finalUtilityThreshold;
    }
    
    public void setFinalUtilityThreshold(Float finalUtilityThreshold) {
        this.finalUtilityThreshold = finalUtilityThreshold;
    }
}
