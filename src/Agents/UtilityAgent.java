package Agents;

import Ontology.MultiAgentTimetablerOntology;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;

public class UtilityAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    private ArrayList<AID> studentAgents;
    private AID timetabler;
    private int numberOfStudents;
    
    private HashMap<AID, Float> studentUtilities;
    
    private Float rawSystemUtility;
    
    private Float averageRawSystemUtility;
    
    private Float totalSystemUtility;
    
    private Float timeUtilityThresholdReached;
    
    private Integer rawUtilityThreshold;

//    private Float utilityThreshold0;
//    private Float utilityThreshold1;
//    private Float finalUtilityThreshold;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        studentAgents = new ArrayList<AID>();
        
        // Register the the UtilityAgent in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("utilityAgent");
        sd.setName("utilityAgent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        rawUtilityThreshold=10;
//        Object[] args = getArguments();
//        if (args != null && args.length > 0) {
//            rawUtilityThreshold = (Integer) args[0];
//
//            System.out.println("utilityThreshold loaded");
//        }
//        if (args != null && args.length > 0) {
//            utilityThreshold0 = (Float) args[0];
//
//            System.out.println("utilityThreshold0 loaded");
//        }
//        if (args != null && args.length > 0) {
//            utilityThreshold1 = (Float) args[0];
//
//            System.out.println("utilityThreshold1 loaded");
//        }
//        if (args != null && args.length > 0) {
//            finalUtilityThreshold = (Float) args[0];
//
//            System.out.println("finalUtilityThreshold loaded");
//        }
//        else {
//// Make the agent terminate immediately
//            System.out.println("No utilityThreshold found");
//            doDelete();
//        }
        
        System.out.println("Waiting for student agents' registration...");
        addBehaviour(new RegistrationReceiver());
        addBehaviour(new UtilityPoller());
        addBehaviour(new NotifyEndByTime(this, 50000));
    }
    
    //registers student agents & sends request inform if
    private class RegistrationReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getConversationId().equals("register")) {
                if (msg.getSender().getName() == "timetabler") {
                    timetabler = msg.getSender();
                }
                else {
                    studentAgents.add(msg.getSender());
                    
                    var reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.addReceiver(msg.getSender());
                    reply.setLanguage(codec.getName());
                    reply.setOntology(ontology.getName());
                    
                    reply.setConversationId("register");
                    reply.setContent(Integer.toString(rawUtilityThreshold));
                    // Let JADE convert from Java objects to string
                    myAgent.send(reply);
                    
                    numberOfStudents = studentAgents.size();
                    
                }
            }
            else {
                System.out.println("Unknown/null message received");
                block();
            }
            
        }
        
    }
    
    public class UtilityPoller extends CyclicBehaviour
    {
        @Override
        public void action() {
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("register"));
            var msg = myAgent.receive(mt);
            
            if (msg != null) {
                var currentStudentRawUtility = Float.parseFloat(msg.getContent());
                
                studentUtilities.put(msg.getSender(), currentStudentRawUtility);
                
            }
            else {
                System.out.println("Unknown/null message received");
                block();
            }
            
        }
    }
    
    public class NotifyEndByTime extends WakerBehaviour
    {
        
        public NotifyEndByTime(Agent a, long timeout) {
            super(a, timeout);
        }
        
        @Override
        public void onWake() {
            var endMsg = new ACLMessage(ACLMessage.INFORM);
            endMsg.setConversationId("end");
            
            studentAgents.forEach(studentAgent -> {
                
                endMsg.addReceiver(studentAgent);
                myAgent.send(endMsg);
                
                var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("end"));
                var msg = myAgent.receive(mt);
                
                if (msg != null && msg.getSender() == studentAgent) {
                    var studentUtility = Float.parseFloat(msg.getContent());
                    studentUtilities.put(studentAgent, studentUtility);
                }
                
            });
            
            endMsg.addReceiver(timetabler);
            myAgent.send(endMsg);
            
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("end"));
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender() == timetabler) {
                var timeSwapBehaviourEnded = Long.parseLong(msg.getContent());
                
                System.out.println("Swap Behaviour ran for: " + timeSwapBehaviourEnded + " seconds");
                
            }
            
            rawSystemUtility = studentUtilities.values().stream().reduce((float) 0, Float::sum);
            
            averageRawSystemUtility = rawSystemUtility / numberOfStudents;
            
            System.out.println("rawSystemUtility: " + rawSystemUtility);
            System.out.println("averageRawSystemUtility: " + averageRawSystemUtility);
            
            takeDown();
            
        }
    }
    
    protected void takeDown()
    {
        
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        System.out.println("Timetabler " + getAID().getName() + " terminating.");
    }
}