package Ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.Ontology;

public class MultiAgentTimetablerOntology extends BeanOntology
{
    public static final String NAME = "MATOntology";
    
    private static Ontology ontologyInstance = new MultiAgentTimetablerOntology();
    
    // The singleton instance of the Time-Ontology
    private static MultiAgentTimetablerOntology theInstance = new MultiAgentTimetablerOntology();
    
    public static Ontology getInstance() {
        return theInstance;
    }
    
    private MultiAgentTimetablerOntology() {
        super(NAME);
        
        try {
            // Add all Concepts, Predicates and AgentActions in the local package
            add(getClass().getPackage().getName());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
}
