package Agents;

import FIPA.AgentID;
import Ontology.Elements.Concepts.Module;
import Ontology.Elements.Concepts.Preference;
import Ontology.Elements.Concepts.Timeslot;
import jade.core.AID;
import jade.core.Agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class StudentAgent extends Agent
{
    private AID aid;
    
    private int matriculationNumber;
    
    private ArrayList<Module> modules;

    private Hashtable<Timeslot, Preference>TimeslotPreferences;
    
    private Hashtable<Timeslot, Assignment>TutorialAssignments;
    
}
