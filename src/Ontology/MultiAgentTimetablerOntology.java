package Ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class MultiAgentTimetablerOntology extends BeanOntology
{
    private static Ontology ontologyInstance = new MultiAgentTimetablerOntology("my_ontology");
    
    public static Ontology getInstance() {
        return ontologyInstance;
    }
    
    //singleton pattern
    private MultiAgentTimetablerOntology(String name) {
        super(name);
        try {
            add("Ontology.Elements");
        }
        catch (BeanOntologyException e) {
            e.printStackTrace();
        }
    }
}
