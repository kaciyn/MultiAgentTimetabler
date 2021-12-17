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
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

public class TimetablerAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    
    private TutorialTimetable timetable;
    private HashMap<AID, Student> studentAgents;
    private HashSet<Student> students;
    private HashMap<Integer, IsUnwanted> unwantedTutorials;
    private HashMap<Integer, Tutorial> tutorialsOnOffer;
    //    private HashMap<AID, Integer> unwantedSlots;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        studentAgents = new HashMap<AID, Student>();
        unwantedTutorials = new HashMap<Integer, IsUnwanted>();
        tutorialsOnOffer = new HashMap<Integer, Tutorial>();
        
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
        addBehaviour(new StudentRegistrationReceiver());
        
       
    }
    
    public class SwapServerBehaviour extends ParallelBehaviour
    {
        public SwapServerBehaviour() {
            super(ParallelBehaviour.WHEN_ALL);
        }
    
        
        @Override
        public void onStart() {
            System.out.println("Accepting swap requests...");
            addBehaviour(new UnwantedSlotReceiver());
            addBehaviour(new SwapOfferReceiver());
    
        }
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
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            
            ACLMessage msg = receive(mt);
            if (msg != null) {
                var studentAID = msg.getSender();
                
                var reply = msg.createReply();
                reply.setLanguage(codec.getName());
                reply.setOntology(ontology.getName());
                reply.setConversationId("list-unwanted-slot");
                reply.addReceiver(studentAID);
                //TODO CHECK IF THERE ARE OTHER REQUESTS COMING IN BC WE DON'T WANNA JUST REJECT THEM LOL
                if (msg.getConversationId().equals("list-unwanted-slot")) {
                    ContentElement contentElement;
                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    try {
                        contentElement = getContentManager().extractContent(msg);
                        
                        if (contentElement instanceof Predicate) {
                            var predicate = ((Predicate) contentElement);
                            
                            if (predicate instanceof IsUnwanted) {
                                var isUnwanted = (IsUnwanted) predicate;
                                //creates an id to reference the unwanted slot offer by so the offering student's identity is not revealed
                                var unwantedId = ThreadLocalRandom.current().nextInt();
                                
                                unwantedTutorials.put(unwantedId, isUnwanted);
                                tutorialsOnOffer.put(unwantedId, isUnwanted.getTutorial());
                                
                                reply.setPerformative(ACLMessage.AGREE);
                                reply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                
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
                    
                }
                else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Invalid request");
                    block();
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
                var broadcast = new ACLMessage(ACLMessage.CFP);
                broadcast.setLanguage(codec.getName());
                broadcast.setOntology(ontology.getName());
                broadcast.addReceiver(studentAgent);
                broadcast.setConversationId("unwanted-slots");
                //todo -> add module to timeslot too + add checks to ensure each student in the correct amount of tutorials?
                
                var areOnOffer = new AreOnOffer();
                areOnOffer.setUnwantedTutorials(tutorialsOnOffer);
                
                try {
                    // Let JADE convert from Java objects to string
                    getContentManager().fillContent(broadcast, areOnOffer);
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
    
    private class SwapOfferReceiver extends CyclicBehaviour
    {
        private OfferSwap offerSwap;
        
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
            
            ACLMessage msg = receive(mt);
            if (msg != null && msg.getConversationId().equals("offer-timeslot-swap")) {
                
                System.out.println(msg.getContent()); //print out the message content in SL
                
                var offeringStudentAID = msg.getSender();
                
                try {
                    var contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof Action) {
                        var action = ((Action) contentElement).getAction();
                        
                        if (action instanceof OfferSwap) {
                            offerSwap = (OfferSwap) action;
                            var offerId = offerSwap.getOfferId();
                            var offeredTutorial = offerSwap.getOfferedTutorial();
                            
                            var reply = msg.createReply();
                            reply.setPerformative(ACLMessage.AGREE);
                            reply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                            reply.addReceiver(offeringStudentAID);
                            reply.setLanguage(codec.getName());
                            reply.setOntology(ontology.getName());
                            reply.setConversationId("list-unwanted-slot");
                            
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
                
            }
            else {
                block();
            }
            
        }
    }
    
    private class SwapOfferReceiver extends OneShotBehaviour
    {
        private class UnwantedSlotListBroadcaster extends OneShotBehaviour
        {
            @Override
            public void action() {
                studentAgents.forEach((studentAgent, student) -> {
                    var broadcast = new ACLMessage(ACLMessage.CFP);
                    broadcast.setLanguage(codec.getName());
                    broadcast.setOntology(ontology.getName());
                    broadcast.addReceiver(studentAgent);
                    broadcast.setConversationId("unwanted-slots");
                    //todo -> add module to timeslot too + add checks to ensure each student in the correct amount of tutorials?
                    
                    var areOnOffer = new AreOnOffer();
                    areOnOffer.setUnwantedTutorials(tutorialsOnOffer);
                    
                    try {
                        // Let JADE convert from Java objects to string
                        getContentManager().fillContent(broadcast, areOnOffer);
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


