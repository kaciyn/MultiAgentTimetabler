package Agents;

import Ontology.Elements.*;
import Ontology.MultiAgentTimetablerOntology;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
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
import java.util.HashSet;

public class TimetablerAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    
    private TutorialTimetable timetable;
    private HashMap<AID, Student> studentAgents;
    private HashSet<Student> students;
    private ArrayList<UnwantedTimeslot> unwantedSlots;
    //    private HashMap<AID, Integer> unwantedSlots;
    private ArrayList<TimeslotId> unwantedSlotList;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        studentAgents = new HashMap<AID, Student>();
        unwantedSlots = new ArrayList<UnwantedTimeslot>();
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
                addBehaviour(new SwapServer());
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
    
    private class SwapServer extends CyclicBehaviour
    {
        @Override
        public void action() {
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
        
        public void broadcastUnwantedSlotList() {
            studentAgents.forEach(studentAgent -> {
                ACLMessage broadcast = new ACLMessage(ACLMessage.INFORM);
                broadcast.setLanguage(codec.getName());
                broadcast.setOntology(ontology.getName());
                broadcast.addReceiver(studentAgent);
    
                //TODO DO I NEED TO MAKE THIS A SOMETHING UGHGFHUFGHGF I'M GONNA DIE
                    try {
                    // Let JADE convert from Java objects to string
                    getContentManager().fillContent(broadcast, isAssignedTo);
                    myAgent.send(broadcast);
                }
                catch (Codec.CodecException ce) {
                    ce.printStackTrace();
                }
                catch (OntologyException oe) {
                    oe.printStackTrace();
                }
                
                broadcast.setContent();
                broadcast.setConversationId("register");
                
                myAgent.send(broadcast);
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
            switch (step) {
                case 0:
                    item = catalogue.get(currentItemIndex);
                    System.out.println(currentItemIndex);
                    
                    System.out.println("Auctioning item: " + item.Description);
                    
                    // Send the cfp to all bidders
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
//iterate over bidder hashset
                    for (AID bidder : studentAgents) {
                        cfp.addReceiver(bidder);
                    }
                    
                    cfp.setContent(item.Description);
                    cfp.setConversationId("auction-item");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value myAgent.send(cfp);
// Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bid-on-item"),
                                             MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    
                    myAgent.send(cfp);
                    
                    step = 1;
                    break;
                case 1:
                    // The counter of replies from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) { // This is an offer
                            int bid = Integer.parseInt(reply.getContent());
                            if (highestBidder == null || bid > highestBid) {
                                // This is the highest bid at present
                                highestBid = bid;
                                highestBidder = reply.getSender();
                            }
                        }
                        else {
                            reply.getPerformative();
                        }
                        responseCount++;
                        if (responseCount >= studentAgents.size()) {
                            // received all responses
                            if (highestBidder == null || highestBid < item.StartingPrice) {
                                step = 3;
                            }
                            else {
                                step = 2;
                                
                            }
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    // Send confirmation to bidder
                    ACLMessage bidConfirmation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    bidConfirmation.addReceiver(highestBidder);
                    bidConfirmation.setContent(item.Description + "," + highestBid);
                    bidConfirmation.setConversationId("bid-successful");
                    bidConfirmation.setReplyWith("win" + System.currentTimeMillis());
                    myAgent.send(bidConfirmation);
                    
                    System.out.println(item.Description + " has been bought by: " + highestBidder + " for " + highestBid);
                    
                    incrementItem();
                    
                    break;
                case 3:
                    //unsold item
                    System.out.println(item.Description + " has not been bid for, or has not met starting price. Auctioning next item");
                    
                    incrementItem();
                    break;
                case 4:
                    //end auction
                    System.out.println("All items bid for. Auction concluded");
                    
                    // Send the cfp to all bidders
                    ACLMessage endNotification = new ACLMessage(ACLMessage.INFORM);
//iterate over bidder hashset
                    for (AID bidder : studentAgents) {
                        endNotification.addReceiver(bidder);
                    }
                    
                    endNotification.setConversationId("auction-concluded");
                    myAgent.send(endNotification);
                    
                    myAgent.doDelete();
                    step = 5;
                    break;
                
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


