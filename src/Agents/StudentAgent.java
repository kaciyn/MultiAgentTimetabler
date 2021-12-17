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
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    
    private AID aid;
    private Student student;
    private int utilityThreshold;
    private int totalUtility;
    final StudentTimetablePreferences timetablePreferences = student.getStudentTimetablePreferences();
    
    private AID timetablerAgent;
    //todo remove the tutorials from the student concept in ontology
    //keeps collection of assigned tutorials and whether they are locked due to having been offered for swap
    private HashMap<Tutorial, Boolean> assignedTutorials;
    //todo
    //    private HashMap<TutorialSlot, Integer> assignedTutorialSlotsWithUtilities;
//    private LinkedHashMap<TutorialSlot, Integer> assignedTutorialSlotsSortedByUtilities;
    
    private HashMap<Integer, Tutorial> unwantedTutorialsOnOffer;
    
    private int minimumSwapUtilityGain;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

// Printout a welcome message
        System.out.println("Hello! Student " + getAID().getName() + "is ready.");
        
        addBehaviour(new TickerBehaviour(this, 10000)
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
                //could customise minimum swap utility gain to adjust strategy
                minimumSwapUtilityGain = 1;
                
                // Register for auction
                myAgent.addBehaviour(new TimetablerRegistrationServer());

//                myAgent.addBehaviour(new InitialTutorialPreferenceMapper());
                myAgent.addBehaviour(new ListUnwantedSlotRequester());
                myAgent.addBehaviour(new SwapOfferResultReceiver());
                
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
                        
                        isAssignedTo.getTutorials().forEach(tutorial -> {
                            assignedTutorials.put(tutorial, false);
                        });
                        
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

//    private class InitialTutorialPreferenceMapper extends OneShotBehaviour
//    {
//        public void action()
//        {
//            assignedTutorials.forEach(tutorial -> {
//                var timeslotUtility = timetablePreferences.getTimeslotUtility(tutorial.getTimeslotId());
//                assignedTutorialSlotsSortedByUtilities.put(tutorial, timeslotUtility);
//            });
//        }
//
//    }
    
    //requests unwanted slots be listed for offers
    private class ListUnwantedSlotRequester extends Behaviour
    {
        private boolean finished = false;
        
        public void action()
        {
            while (totalUtility < utilityThreshold) {
                
                assignedTutorials.forEach((tutorial, isLocked) -> {
                    if (timetablePreferences.getTimeslotUtility(tutorial.getTimeslotId()) < 0 && !isLocked) {
                        var unwantedSlot = new IsUnwanted();
                        unwantedSlot.setStudentAID(aid);
                        unwantedSlot.setTutorial(tutorial);
                        
                        //todo issue with this is that it's offering up all the negative utility slots right off the bat which doesn't leave any to offer up
                        //todo maybe undesired slots from the same module can be automatically swapped out by timetabler agent? no we need to check that the other slot also isn't undesired - just make option to retract offer?
                        var offer = new ACLMessage(ACLMessage.REQUEST);
                        offer.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                        
                        offer.setLanguage(codec.getName());
                        offer.setOntology(ontology.getName());
                        offer.addReceiver(timetablerAgent);
                        offer.setConversationId("list-unwanted-slot");
                        
                        try {
                            // Let JADE convert from Java objects to string
                            getContentManager().fillContent(offer, unwantedSlot);
                            myAgent.send(offer);
                            
                            //receive response
                            var mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
                            var reply = myAgent.receive(mt);
                            
                            if (reply == null || !reply.getConversationId().equals("list-unwanted-slot")) {
                                //todo again check if this sticks
                                isLocked = true;
                            }
                            
                        }
                        catch (
                                Codec.CodecException ce) {
                            ce.printStackTrace();
                        }
                        catch (
                                OntologyException oe) {
                            oe.printStackTrace();
                        }
                        
                        //todo vv something along these lines so we don't go swapping it away
                        //TODO WOULD BE NICE TO IMPLEMENT THE OPTION TO RESCIND OFFER AND STICK IT BACK INTO THE POOL AFTER TIMEOUT actually this may be necessary to stop the system from getting stuck - a waker behaviour?
                    }
                });
            }
            finished = true;
            done();
        }
        
        //TODO CHECK IF THIS IS NECESSARY
        @Override
        public boolean done() {
            return finished;
        }
    }
    
    private class UnwantedSlotListReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            var msg = myAgent.receive(mt);
            
            if (msg.getSender() == timetablerAgent && msg != null && msg.getConversationId().equals("unwanted-slots")) {
                //receive response
                
                try {
                    ContentElement contentElement = null;
                    
                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof AreOnOffer) {
                        var areOnOffer = (AreOnOffer) contentElement;
                        
                        unwantedTutorialsOnOffer = areOnOffer.getUnwantedTutorials();
                        
                        myAgent.addBehaviour(new SwapOfferProposer());
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
                System.out.println("Unknown/null message received");
                block();
            }
        }
    }
    
    private class SwapOfferProposer extends OneShotBehaviour
    {
//            public SwapOfferProposer(Agent a, long period) {
//                super(a, period);
//            }
        
        //            protected void onTick() {
        @Override
        public void action() {
            var timetablePreferences = student.getStudentTimetablePreferences();
            
            assignedTutorials.forEach((currentTutorial, isLocked) -> {
                var currentTimeslotId = currentTutorial.getTimeslotId();
                
                //TODO CHECK THIS WORKS AS EXPECTED
                //filters slots on offer for tutorials of the same module as current tutorial slot
                var unwantedTutorialsOnOfferFiltered = unwantedTutorialsOnOffer.entrySet()
                                                                               .stream()
                                                                               .filter(map -> !currentTimeslotId.equals(map.getValue().getTimeslotId()))
                                                                               .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
                
                if (unwantedTutorialsOnOfferFiltered.size() > 0) {
                    Integer bestSwapId = null;
                    var bestUtilityChange = minimumSwapUtilityGain;
                    
                    // Iterating HashMap through for loop
                    for (Map.Entry<Integer, Tutorial> set :
                            unwantedTutorialsOnOfferFiltered.entrySet()) {
                        
                        var tutorial = set.getValue();
                        var offerId = set.getKey();
                        
                        var offeredUtility = timetablePreferences.getTimeslotUtility(tutorial.getTimeslotId());
                        var utilityChange = offeredUtility - timetablePreferences.getTimeslotUtility(currentTimeslotId);
                        
                        if (utilityChange > bestUtilityChange) {
                            bestSwapId = offerId;
                            bestUtilityChange = utilityChange;
                        }
                    }
                    
                    if (bestSwapId != null) {
                        
                        // Prepare the action request message
                        var message = new ACLMessage(ACLMessage.PROPOSE);
                        message.addReceiver(timetablerAgent);
                        //slightly truncated contract net
                        //TODO CHECK ABOVE ASSERTION IS TRUE
                        message.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                        message.setLanguage(codec.getName());
                        message.setOntology(ontology.getName());
                        message.setConversationId("offer-timeslot-swap");
                        
                        var offerSwap = new OfferSwap();
                        offerSwap.setOfferingStudentAID(aid);
                        offerSwap.setOfferId(offerSwap.getOfferId());
                        offerSwap.setOfferedTutorial(currentTutorial);
                        
                        //locks tutorial so it isn't offered somewhere else
                        assignedTutorials.put(currentTutorial, true);
                        
                        var offer = new Action();
                        offer.setAction(offerSwap);
                        offer.setActor(timetablerAgent); // the agent that you request to perform the action
                        try {
                            // Let JADE convert from Java objects to string
                            getContentManager().fillContent(message, offer); //send the wrapper object
                            send(message);
                        }
                        catch (Codec.CodecException ce) {
                            ce.printStackTrace();
                        }
                        catch (OntologyException oe) {
                            oe.printStackTrace();
                        }
                    }
                }
            });
        }
    }
    
    private class SwapOfferResultReceiver extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            //receive response
            var mt = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchSender(timetablerAgent));
            
            var reply = myAgent.receive(mt);
            
            if (reply != null && reply.getConversationId().equals("timeslot-swap-proposal")) {
                try {
                    ContentElement contentElement = null;
                    
                    System.out.println(reply.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (contentElement instanceof IsSwapResult) {
                        var isSwapResult = (IsSwapResult) contentElement;
                        
                        var offeredTutorial = isSwapResult.getOfferedTutorial();
                        var requestedTutorial = isSwapResult.getRequestedTutorial();
                        
                        if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            //removes offered tutorial and adds new tutorial
                            assignedTutorials.remove(offeredTutorial);
                            assignedTutorials.put(requestedTutorial, false);
                        }
                        else if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                            //unlocks tutorial slot
                            assignedTutorials.put(offeredTutorial, false);
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
            
        }
    }
}
