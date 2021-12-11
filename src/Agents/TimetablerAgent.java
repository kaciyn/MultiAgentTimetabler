package Agents;

import Ontology.Elements.*;
import Ontology.MultiAgentTimetablerOntology;
import jade.content.ContentElement;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TimetablerAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    
    private TutorialTimetable timetable;
    private HashMap<AID, Student> studentAgents;
    private HashSet<Student> students;
    private ArrayList<Tutorial> unwantedSlots;
    //    private HashMap<AID, Integer> unwantedSlots;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        studentAgents = new HashMap<AID, Student>();
        unwantedSlots = new ArrayList<IsUnwanted>();
//        unwantedSlots = new HashMap<AID, Integer>();
        unwantedSlotList = new ArrayList<TimeslotId>();
        
        // Register the the timetabler in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("timetabler");
        sd.setName("timetabler");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
//get args
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            timetable = (TutorialTimetable) args[0];
            
            System.out.println("Initial timetable loaded");
        }
        if (args != null && args.length > 1) {
            students = (HashSet<Student>) args[1];
            
            System.out.println("Students loaded");
        }
        else {
// Make the agent terminate immediately
            System.out.println("No timetable or students found");
            doDelete();
        }
        
        System.out.println("Waiting for student agents' registration...");
        //TODO PICKUP HERE
        addBehaviour(new StudentRegistrationReceiver());
        
        addBehaviour(new WakerBehaviour(this, 6000)
        {
            protected void handleElapsedTimeout()
            {
                System.out.println("Accepting swap requests...");
                addBehaviour(new UnwantedSlotReceiver());
            }
        });
        
    }
    
    //registers student agents and sends them their initial timetable, links student with aid
    private class StudentRegistrationReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getConversationId().equals("register")) {
                
                AID newStudentAID = msg.getSender();
                var newStudentMatric = Integer.parseInt(msg.getContent());
                var newStudent = students.stream().filter(student -> newStudentMatric == student.getMatriculationNumber()).findFirst().orElse(null);
                
                var reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.addReceiver(newStudentAID);
                reply.setLanguage(codec.getName());
                reply.setOntology(ontology.getName());
                
                if (newStudent == null) {
                    
                    reply.setContent("Student is not enrolled");
                    
                    System.out.println(newStudentAID.getName() + " not registered ");
                    
                    myAgent.send(reply);
                    return;
                }
                
                studentAgents.put(newStudentAID, newStudent);
                
                var studentTutorials = newStudent.getTutorials();
                
                var isAssignedTo = new IsAssignedTo();
                isAssignedTo.setAttendingStudent(newStudent);
                isAssignedTo.setTutorials(studentTutorials);
                
                try {
                    // Let JADE convert from Java objects to string
                    getContentManager().fillContent(reply, isAssignedTo);
                    myAgent.send(reply);
                }
                catch (Codec.CodecException ce) {
                    ce.printStackTrace();
                }
                catch (OntologyException oe) {
                    oe.printStackTrace();
                }

//                ACLMessage reply = msg.createReply();
//                reply.setPerformative(ACLMessage.INFORM);
//                reply.setContent("Registration confirmed");
//
//                System.out.println(newStudentAID.getName() + " registered ");
//
//                myAgent.send(reply);
            
            }
            else {
                System.out.println("Unknown/null message received");
                block();
            }
            
        }
        
    }
    
    private class UnwantedSlotReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = receive(mt);
            if (msg != null) {
                try {
                    ContentElement contentElement = null;
                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    var studentAID = msg.getSender();
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    try {
                        contentElement = getContentManager().extractContent(msg);
                        
                        if (contentElement instanceof Predicate) {
                            var predicate = ((Predicate) contentElement);
                            
                            if (predicate instanceof IsUnwanted) {
                                var isUnwanted = (IsUnwanted) predicate;
                                unwantedSlots.add(isUnwanted);
                                
                                addBehaviour(new UnwantedSlotListBroadcaster());
                            }
                        }
                    }
                    catch (Codec.CodecException e) {
                        e.printStackTrace();
                    }
                    catch (OntologyException e) {
                        e.printStackTrace();
                    }
                    
                    catch (Codec.CodecException ce) {
                        ce.printStackTrace();
                    }
                }
                catch (OntologyException oe) {
                    oe.printStackTrace();
                }
                
            }
            else {
                block();
            }
            
        }
    }
    
    private class UnwantedSlotListBroadcaster extends OneShotBehaviour
    {
        @Override
        public void action() {
            studentAgents.forEach((studentAgent, student) -> {
                var broadcast = new ACLMessage(ACLMessage.INFORM);
                broadcast.setLanguage(codec.getName());
                broadcast.setOntology(ontology.getName());
                broadcast.addReceiver(studentAgent);
                broadcast.setConversationId("unwanted-slots");
                //todo consider changing this or at least checking bc this seems redundant in that you're potensh sending tutorials in twice - decouple student entirely and just have timetabler keep map of students/tutorial slots
                //todo -> add module to timeslot too + add checks to ensure each student in the correct amount of tutorials?
                
                try {
                    // Let JADE convert from Java objects to string
                    getContentManager().fillContent(broadcast, isUnwanted);
                    myAgent.send(broadcast);
                }
                catch (Codec.CodecException ce) {
                    ce.printStackTrace();
                }
                catch (OntologyException oe) {
                    oe.printStackTrace();
                }
                
            });
        }
        
    }
    
    private class SwapServerr extends Behaviour
    {
        private AID highestBidder = null; // The agent who provides the best offer
        private int highestBid = 0; // The best offered price
        int responseCount = 0; // The counter of replies from seller agents                    // Receive all bids/refusals from bidder
        
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        Item item;
        
        public void action()
        {
            //This behaviour should only respond to REQUEST messages
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);
            if (msg != null) {
                try {
                    ContentElement contentElement = null;
                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    var studentAID = msg.getSender();
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof Action) {
                        var action = ((Action) contentElement).getAction();
                        
                        if (action instanceof RequestSwap) {
                            var requestedSwap = (RequestSwap) action;
                            Event unwantedEvent = requestedSwap.getUnwantedTutorial();
                            
                            if (unwantedEvent instanceof Tutorial) {
                                
                                var unwantedTutorial = (Tutorial) unwantedEvent;
                                var unwantedTimeSlotId = new TimeslotId(unwantedTutorial.getTimeslotId());
                                var unwantedTimeSlot = new UnwantedTimeslot(studentAID, unwantedTutorial.getTimeslotId());
                                
                                unwantedSlots.add(unwantedTimeSlot);
                                unwantedSlotList.add(unwantedTimeSlotId);
                                
                                broadcastUnwantedSlotList();
                            }
                        }
                        
                    }
                }
                
                catch (Codec.CodecException ce) {
                    ce.printStackTrace();
                }
                catch (OntologyException oe) {
                    oe.printStackTrace();
                }
                
            }
            else {
                block();
            }
        }
        
    }
    
    //resets for next item, ends auction if at end of catalogue
    void incrementItem()
    {
        //reset for next item
        responseCount = 0;
        highestBidder = null; // The agent who provides the best offer
        highestBid = 0; // The best offered price
        currentItemIndex++;
        
        if (currentItemIndex >= catalogue.size()) {
            step = 4;
            
        }
        else {
            step = 0;
        }
        
    }
    
    public boolean done()
    {
        return (step == 5);
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
        
        System.out.println("Auctioneer " + getAID().getName() + " terminating.");
    }
}


