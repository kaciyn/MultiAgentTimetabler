package Ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class MultiAgentTimetablerOntology extends BeanOntology
{
    private static Ontology ontologyInstance = new MultiAgentTimetablerOntology("MATOntology");
    
    public static Ontology getInstance(){
        return ontologyInstance;
    }
    //singleton pattern
    private MultiAgentTimetablerOntology(String name) {
        super(name);
        try {
            add("Ontology.Elements");
        } catch (BeanOntologyException e) {
            e.printStackTrace();
        }
    }
    
    
//    public static final String NAME = "MATOntology";
//
//    private static Ontology ontologyInstance = new MultiAgentTimetablerOntology();
//
//    // The singleton instance of the Ontology
//    private static MultiAgentTimetablerOntology theInstance = new MultiAgentTimetablerOntology();
//
//    public static Ontology getInstance() {
//        return theInstance;
//    }
//
//    private MultiAgentTimetablerOntology() {
//        super(NAME);
//
//        try {
//            // Add all Concepts, Predicates and AgentActions in the local package
//            add(getClass().getPackage().getName());
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
    }
    
    

