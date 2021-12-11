package Agents;

import Ontology.Elements.*;
import Ontology.MultiAgentTimetablerOntology;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.annotations.Slot;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;

public class StudentAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    
    @Slot(mandatory = true)
    private AID aid;
    
    private Student student;
    
    private AID timetablerAgent;
    //todo remove the tutorials from the student concept in ontology
    private ArrayList<Tutorial> assignedTutorials;
    private HashMap<Tutorial, Integer> assignedTutorialsWithUtilities;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

// Printout a welcome message
        System.out.println("Hello! Student " + getAID().getName() + "is ready.");
        
        addBehaviour(new TickerBehaviour(this, 1000)
        {
            @Override
            protected void onTick()
            {
                // Search for timetabler agent
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("register");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result.length > 0) {
                        timetablerAgent = result[0].getName();
                    }
                    
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                
                // Register for auction
                myAgent.addBehaviour(new TimetablerRegistrationServer());
                
                //todo NEXT do thing where it looks at own prefs and figures out what it doesn't want/ maps slots to the utilities
                myAgent.addBehaviour(new InitialTutorialPreferenceMapper());
                myAgent.addBehaviour(new UnwantedSlotAdvertiser());
                myAgent.addBehaviour(new UnwantedSlotAdvertiser());
                
            }
        });
    }
    
    //inform timetabler agent it wishes to register
    private class TimetablerRegistrationServer extends OneShotBehaviour
    {
        @Override
        public void action()
        {
            ACLMessage registration = new ACLMessage(ACLMessage.INFORM);
            registration.addReceiver(timetablerAgent);
            //send matric to timetabler to register
            registration.setContent(Integer.toString(student.getMatriculationNumber()));
            registration.setConversationId("register");
            
            myAgent.send(registration);
            
            //receive response
            var mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var reply = myAgent.receive(mt);
            
            if (reply != null && reply.getConversationId().equals("register")) {
                try {
                    ContentElement contentElement = null;
                    
                    System.out.println(reply.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (contentElement instanceof IsAssignedTo) {
                        var isAssignedTo = (IsAssignedTo) contentElement;
                        
                        assignedTutorials = isAssignedTo.getTutorials();
                        
                    }
                    
                }
                catch (Codec.CodecException ce) {
                    ce.printStackTrace();
                }
                catch (OntologyException oe) {
                    oe.printStackTrace();
                }
                
            }
        }
        
    }
    
    private class InitialTutorialPreferenceMapper extends OneShotBehaviour
    {
        public void action()
        {
            assignedTutorials.forEach(tutorial -> {
                var timetablePreferences = student.getStudentTimetablePreferences();
                var timeslotUtility = timetablePreferences.getTimeslotUtility(tutorial.getTimeslotId());
                assignedTutorialsWithUtilities.put(tutorial, timeslotUtility);
            });
        }
        
    }
    
    //tries to get rid of unwanted slots
    private class UnwantedSlotAdvertiser extends OneShotBehaviour
    {
        public void action()
        {
            assignedTutorialsWithUtilities.forEach((tutorial, utility) -> {
                if (utility < 0) {
                    var unwantedSlot = new IsUnwanted();
                    unwantedSlot.setStudentAID(aid);
                    unwantedSlot.addTutorial(tutorial);
                    //todo issue with this is that it's offering up all the negative utility slots right off the bat which doesn't leave any to offer up
                    //todo maybe undesired slots from the same module can be automatically swapped out by timetabler agent? no we need to check that the other slot also isn't undesired - just make option to retract offer?
                    var offer = new ACLMessage(ACLMessage.INFORM);
                    offer.setLanguage(codec.getName());
                    offer.setOntology(ontology.getName());
                    offer.addReceiver(timetablerAgent);
                    offer.setConversationId("offer-unwanted-slot");
                    
                    try {
                        // Let JADE convert from Java objects to string
                        getContentManager().fillContent(offer, unwantedSlot);
                        myAgent.send(offer);
                    }
                    catch (Codec.CodecException ce) {
                        ce.printStackTrace();
                    }
                    catch (OntologyException oe) {
                        oe.printStackTrace();
                    }
                    
                    //todo maybe get a confirmation of receipt and posting before listing it as blocked
                    //todo i think i need to rethink the classes bc this is terrible.
                    //todo vv something along these lines so we don't go swapping it away
                    tutorialSlot.isOnOffer();
                }
            });
        }
        
    }
    
    private class OfferedSlotListReceiver extends CyclicBehaviour
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
    
    private class RecklessBuyerBehaviour extends TickerBehaviour
    {
        public RecklessBuyerBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        protected void onTick() {
            // Prepare the action request message
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(sellerAID); // sellerAID is the AID of the Seller agent
            msg.setLanguage(codec.getName());
            msg.setOntology(ontology.getName());
            // Prepare the content.
            CD cd = new CD();
            cd.setName("Synchronicity");
            cd.setSerialNumber(123);
            ArrayList<Track> tracks = new ArrayList<Track>();
            Track t = new Track();
            t.setName("Every breath you take");
            t.setDuration(230);
            tracks.add(t);
            t = new Track();
            t.setName("King of pain");
            t.setDuration(500);
            tracks.add(t);
            cd.setTracks(tracks);
            Sell order = new Sell();
            order.setBuyer(myAgent.getAID());
            order.setItem(cd);
            
            //IMPORTANT: According to FIPA, we need to create a wrapper Action object
            //with the action and the AID of the agent
            //we are requesting to perform the action
            //you will get an exception if you try to send the sell action directly
            //not inside the wrapper!!!
            Action request = new Action();
            request.setAction(order);
            request.setActor(sellerAID); // the agent that you request to perform the action
            try {
                // Let JADE convert from Java objects to string
                getContentManager().fillContent(msg, request); //send the wrapper object
                send(msg);
            }
            catch (Codec.CodecException ce) {
                ce.printStackTrace();
            }
            catch (OntologyException oe) {
                oe.printStackTrace();
            }
            
        }
        
    }
    
    private class AuctionBidPerformer extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
// Message received. Process it
                Module itemDescription = msg.getContent();
                
                //TODO nothing to force format of message received which is not good or even check for whether the second bit is a number lol
//for when the price is relevant later
//                String itemDescription = itemDetails.split(",")[0];
                
                ACLMessage reply = msg.createReply();
                reply.setConversationId("bid-on-item");
                //if item is on shopping list, bid with requisite price
                if (shoppingList.containsKey(itemDescription)) {
//                    int itemPrice = Integer.parseInt(itemDetails.split(",")[1]);
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(Module.valueOf(shoppingList.get(itemDescription)));
                }
                else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }// End of inner class
    
    private class BidResultReceiver extends CyclicBehaviour
    {
        private int step = 0;
        
        public void action()
        {
            switch (step) {
                case 0:
                    
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
// INFORM Message received. Process it
                        
                        if (msg.getConversationId().equals("auction-concluded")) {
                            step = 1;
                        }
                        else if (msg.getConversationId().equals("bid-successful")) {
                            System.out.println("Bidding won by: " + myAgent.getLocalName());
                            var itemDescription = msg.getContent().split(",")[0];
                            var itemPrice = Integer.parseInt(msg.getContent().split(",")[1]);
                            
                            //add item to bought items, remove from shopping list
                            boughtItems.put(itemDescription, itemPrice);
                            shoppingList.remove(itemDescription, itemPrice);
                        }
                        else {
                            System.out.println(myAgent.getLocalName() + "'s bid unsuccessful");
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 1: {
                    System.out.println("Bidder " + getAID().getName() + " purchased " + boughtItems.size() + " items out of the" + shoppingList.size() + " items they wanted.");

// Printout a dismissal message
                    System.out.println("Bidder " + getAID().getName() + "terminating.");
                    
                    // Make the agent terminate immediately
                    doDelete();
                    break;
                }
                
            }
        }
    }
}
