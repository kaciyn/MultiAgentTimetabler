package Agents;

import Ontology.Elements.*;
import Ontology.MultiAgentTimetablerOntology;
import jade.content.ContentElement;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
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
import java.util.Map;
import java.util.stream.Collectors;

public class StudentAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    
    private AID aid;
    private Student student;
    private long utilityThreshold;
    private int totalUtility;
    
    private StudentTimetablePreferences timetablePreferences;
    
    private AID timetablerAgent;
    private AID utilityAgent;
    
    //todo remove the tutorials from the student concept in ontology
    //keeps collection of assigned tutorial slots and whether they are locked due to having been offered for swap
    private HashMap<Long, Boolean> assignedTutorialSlots;
    //todo
    //    private HashMap<TutorialSlot, Long> assignedTutorialSlotsWithUtilities;
//    private LinkedHashMap<TutorialSlot, Long> assignedTutorialSlotsSortedByUtilities;
    
    private HashMap<Long, Long> unwantedTutorialsOnOffer;
    
    private int minimumSwapUtilityGain;
    
    protected void setup()
    {
        utilityThreshold = 1;
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

// Printout a welcome message
        System.out.println("Hello! Student " + getAID().getName() + " is ready.");
        aid = getAID();
        
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            student = (Student) args[0];
        }
        
        assignedTutorialSlots = new HashMap<>();
        timetablePreferences = student.getStudentTimetablePreferences();
        
        addBehaviour(new WakerBehaviour(this, 10000)
        {
            protected void onWake()
            {
                // Search for timetabler agent
                var template = new DFAgentDescription();
                var timetablerSd = new ServiceDescription();
                timetablerSd.setType("timetabler");
                template.addServices(timetablerSd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result.length > 0) {
                        timetablerAgent = result[0].getName();
                    }
                    
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }

//                // Search for utility agent
//                DFAgentDescription utilTemplate = new DFAgentDescription();
//                var utilSd = new ServiceDescription();
//                utilSd.setType("utilityAgent");
//                utilTemplate.addServices(utilSd);
//                try {
//                    DFAgentDescription[] result = DFService.search(myAgent, utilTemplate);
//                    if (result.length > 0) {
//                        utilityAgent = result[0].getName();
//                    }
//                }
//                catch (FIPAException fe) {
//                    fe.printStackTrace();
//                }
//                myAgent.addBehaviour(new UtilityRegistrationServer());
//
//                //could customise minimum swap utility gain to adjust strategy
//                minimumSwapUtilityGain = 1;
                
                // Register with timetabler
                myAgent.addBehaviour(new TimetablerRegistrationServer());

//                while (assignedTutorialSlots.size()<1){
//                    doWait();
//                }
                myAgent.addBehaviour(new SwapBehaviour());
            }
            
        });
    }
    
    //inform timetabler agent it wishes to register
    private class TimetablerRegistrationServer extends OneShotBehaviour
    {
        @Override
        public void action()
        {
            var registration = new ACLMessage(ACLMessage.INFORM);
            registration.addReceiver(timetablerAgent);
            //send matric to timetabler to register
            registration.setContent(java.lang.Long.toString(student.getMatriculationNumber()));
            registration.setConversationId("register");
            
            myAgent.send(registration);
            
            //receive response
            var mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var reply = blockingReceive(mt);
            
            if (reply != null && reply.getConversationId().equals("register")) {
                try {
                    ContentElement contentElement;
                    
                    System.out.println(reply.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (contentElement instanceof IsAssignedTo) {
                        var isAssignedTo = (IsAssignedTo) contentElement;
                        System.out.println(aid.getName() + " received tutorials ");
                        
                        isAssignedTo.getTutorialSlots().forEach(tutorialSlot -> {
                            assignedTutorialSlots.put(tutorialSlot, false);
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
            else {
                block();
            }
        }
        
    }
    
    public class SwapBehaviour extends ParallelBehaviour
    {
        public SwapBehaviour() {
            super(ParallelBehaviour.WHEN_ALL);
        }
        
        @Override
        public void onStart() {
            addSubBehaviour(new UnwantedSlotListReceiver());
            
            addSubBehaviour(new ListUnwantedSlotRequestConfirmationReceiver());
            
            addSubBehaviour(new UnavailableSlotListReceiver());
            
            addSubBehaviour(new OfferSwapResultReceiver());
            
            addSubBehaviour(new RequestSwapUnwantedSlots());
            
            addSubBehaviour(new SwapOfferProposer());
            
            addSubBehaviour(new ProposeSwapReceiver());

//            addSubBehaviour(new UtilitySender(myAgent, 10000));
            
            addSubBehaviour(new EndListener());
            
        }
    }
    
    //for requesting swaps
    public class RequestSwapUnwantedSlots extends ParallelBehaviour
    {
        public RequestSwapUnwantedSlots() {
            super(ParallelBehaviour.WHEN_ALL);
        }
        
        //requests unwanted slots be listed for offers
//    private class ListUnwantedSlotRequester extends Behaviour
        
        @Override
        public void onStart() {
            addSubBehaviour(new ListUnwantedSlotRequestSender());
            addSubBehaviour(new ListUnwantedSlotRequestConfirmationReceiver());
            
        }
    }
    
    public class ListUnwantedSlotRequestSender extends Behaviour
    {
        boolean finished = false;
        
        @Override
        public void action() {
            if (totalUtility < utilityThreshold) {
                assignedTutorialSlots.forEach((tutorialSlot, isLocked) -> {
                    if (timetablePreferences.getTimeslotUtility(tutorialSlot) < 0 && !isLocked) {
                        
                        var unwantedSlot = new IsUnwanted();
                        unwantedSlot.setStudentAID(aid);
                        unwantedSlot.setTutorialSlot(tutorialSlot);
                        
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
                            System.out.println(aid.getName() + " sent unwanted tutorial offer" + unwantedSlot.getTutorialSlot());
                            
                            //puts hold on slot unless the request is rejected
                            assignedTutorialSlots.put(tutorialSlot, true);
                            
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
            else {
                finished = true;
            }
            
        }
        
        @Override
        public boolean done() {
            return finished;
        }
    }
    
    public class ListUnwantedSlotRequestConfirmationReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            //receive response
            var mt = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.and(MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("list-unwanted-slot")));
            
            var reply = myAgent.receive(mt);
            
            ContentElement contentElement;
            
            try {
                if (reply != null && reply.getConversationId().equals("list-unwanted-slot")) {
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (contentElement instanceof Predicate) {
                        var predicate = ((Predicate) contentElement);
                        
                        if (predicate instanceof IsUnwanted) {
                            var isUnwanted = (IsUnwanted) predicate;
                            
                            if (reply.getPerformative() == ACLMessage.AGREE) {
                                System.out.println(aid.getName() + "'s tutorial swap offer received for " + isUnwanted.getTutorialSlot());
                                
                                //locks slot
                                assignedTutorialSlots.put(isUnwanted.getTutorialSlot(), true);
                            }
                            else {
                                System.out.println(aid.getName() + "'s tutorial swap offer rejected, unlocking slot " + isUnwanted.getTutorialSlot());
                                
                                //unlocks slot so offer can be repeated
                                assignedTutorialSlots.put(isUnwanted.getTutorialSlot(), false);
                            }
                            
                        }
                    }
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
        }
    }
    
    private class ProposeSwapReceiver extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            //receive response
            var mt = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchSender(timetablerAgent));
            
            var proposal = myAgent.receive(mt);
            
            if (proposal != null && proposal.getConversationId().equals("timeslot-swap-proposal")) {
                try {
                    ContentElement contentElement;
                    
                    System.out.println(proposal.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(proposal);
                    
                    if (contentElement instanceof Action) {
                        var action = ((Action) contentElement).getAction();
                        if (action instanceof AcceptSwap) {
                            var acceptSwap = (AcceptSwap) action;
                            
                            Long offeredTutorialSlot = acceptSwap.getProposedTutorialSlot();
                            Long unwantedTutorialSlot = acceptSwap.getUnwantedTutorialSlot();
                            
                            var offeredUtility = timetablePreferences.getTimeslotUtility(offeredTutorialSlot);
                            var utilityChange = offeredUtility - timetablePreferences.getTimeslotUtility(unwantedTutorialSlot);
                            
                            var proposalReply = proposal.createReply();
                            
                            if (utilityChange >= minimumSwapUtilityGain) {
                                
                                proposalReply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                
                                //receive response
                                var confirmTemplate = MessageTemplate.and(
                                        MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                        MessageTemplate.MatchSender(timetablerAgent));
                                
                                var confirm = myAgent.receive(confirmTemplate);
                                
                                if (confirm != null && confirm.getConversationId().equals("timeslot-swap-proposal")) {
                                    //removes offered tutorial and adds new tutorial
                                    assignedTutorialSlots.remove(unwantedTutorialSlot);
                                    assignedTutorialSlots.put(Long.valueOf(offeredTutorialSlot), false);
                                    student.removeTutorialSlot(unwantedTutorialSlot);
                                    student.addTutorialSlot(offeredTutorialSlot);
                                    
                                    totalUtility = timetablePreferences.getTotalUtility(student.getTutorialSlots(), timetablePreferences);
                                    
                                }
                            }
                            
                        }
                    }
                }
                
                catch (UngroundedException e) {
                    e.printStackTrace();
                }
                catch (OntologyException e) {
                    e.printStackTrace();
                }
                catch (Codec.CodecException e) {
                    e.printStackTrace();
                }
                
            }
            
        }
    }
    
    //for making swap offers
    private class UnwantedSlotListReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender() == timetablerAgent && msg.getConversationId().equals("unwanted-slot")) {
                //receive response
                
                try {
                    ContentElement contentElement = null;
                    
                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof IsOnOffer) {
                        var isOnOffer = (IsOnOffer) contentElement;
                        
                        unwantedTutorialsOnOffer.put(isOnOffer.getUnwantedTutorialId(), isOnOffer.getUnwantedTutorialSlot());
                        
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
//                System.out.println("Unknown/null message received");
                block();
            }
        }
    }
    
    //for making swap offers
    private class UnavailableSlotListReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender() == timetablerAgent && msg.getConversationId().equals("taken-slot")) {
                //receive response
                
                try {
                    ContentElement contentElement = null;
                    
                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof IsNoLongerOnOffer) {
                        var isNoLongerOnOffer = (IsNoLongerOnOffer) contentElement;
                        
                        unwantedTutorialsOnOffer.remove(isNoLongerOnOffer.getUnavailableTutorialId());
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
//                System.out.println("Unknown/null message received");
                block();
            }
        }
    }
    
    private class SwapOfferProposer extends OneShotBehaviour
    {
        @Override
        public void action() {
            var timetablePreferences = student.getStudentTimetablePreferences();
            
            assignedTutorialSlots.forEach((currentTimeslotId, isLocked) -> {
                
                //TODO CHECK THIS WORKS AS EXPECTED
                //filters slots on offer for tutorials of the same module as current tutorial slot
                var unwantedTutorialsOnOfferFiltered = unwantedTutorialsOnOffer.entrySet()
                                                                               .stream()
                                                                               .filter(map -> !currentTimeslotId.equals(map.getValue()))
                                                                               .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
                
                if (unwantedTutorialsOnOfferFiltered.size() > 0) {
                    java.lang.Long bestSwapId = null;
                    var bestUtilityChange = minimumSwapUtilityGain;
                    
                    // Iterating HashMap through for loop
                    for (Map.Entry<Long, Long> set :
                            unwantedTutorialsOnOfferFiltered.entrySet()) {
                        
                        var tutorialSlot = set.getValue();
                        var offerId = set.getKey();
                        
                        var offeredUtility = timetablePreferences.getTimeslotUtility(tutorialSlot);
                        var utilityChange = offeredUtility - timetablePreferences.getTimeslotUtility(currentTimeslotId);
                        
                        if (utilityChange >= bestUtilityChange) {
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
                        offerSwap.setOfferedTutorialSlot(currentTimeslotId);
                        
                        //locks tutorial so it isn't offered somewhere else
                        assignedTutorialSlots.put(currentTimeslotId, true);
                        
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
    
    private class OfferSwapResultReceiver extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            //receive response
            var mt = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET), MessageTemplate.and(
                            MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("timeslot-swap-proposal")));
            
            var reply = myAgent.receive(mt);
            
            if (reply != null) {
                try {
                    ContentElement contentElement = null;
                    
                    System.out.println(reply.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (contentElement instanceof IsSwapResult) {
                        var isSwapResult = (IsSwapResult) contentElement;
                        
                        Long offeredTutorialSlots = isSwapResult.getOfferedTutorialSlot();
                        Long requestedTutorial = isSwapResult.getRequestedTutorialSlot();
                        
                        if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            //removes offered tutorial and adds new tutorial
                            assignedTutorialSlots.remove(offeredTutorialSlots);
                            assignedTutorialSlots.put(requestedTutorial, false);
                            
                            student.removeTutorialSlot(offeredTutorialSlots);
                            student.addTutorialSlot(requestedTutorial);
                            
                            totalUtility = timetablePreferences.getTotalUtility(student.getTutorialSlots(), timetablePreferences);
                        }
                        else if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                            //unlocks tutorial slot
                            assignedTutorialSlots.put(offeredTutorialSlots, false);
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
    
    private class UtilityRegistrationServer extends OneShotBehaviour
    {
        @Override
        public void action()
        {
            ACLMessage registration = new ACLMessage(ACLMessage.INFORM);
            registration.addReceiver(utilityAgent);
            //send matric to utilityAgent to register
            registration.setConversationId("register");
            
            myAgent.send(registration);
            
            //receive response
            var mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var reply = myAgent.receive(mt);
            
            if (reply != null && reply.getConversationId().equals("register")) {
                utilityThreshold = Long.parseLong(reply.getContent());
                
            }
        }
    }
    
    private class UtilitySender extends TickerBehaviour
    {
        
        public UtilitySender(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            
            var msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(utilityAgent);
            //send matric to utilityAgent to register
            msg.setConversationId("current-utility");
            
            msg.setContent(java.lang.Long.toString(totalUtility));
            myAgent.send(msg);
            
        }
        
    }
    
    private class EndListener extends CyclicBehaviour
    {
        
        @Override
        public void action() {
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("end"));
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender() == utilityAgent) {
                
                System.out.println("Student " + student.getMatriculationNumber() + " raw utility achieved:" + totalUtility);
                
                var utilmsg = new ACLMessage(ACLMessage.INFORM);
                utilmsg.addReceiver(utilityAgent);
                //send matric to utilityAgent to register
                utilmsg.setConversationId("end");
                
                utilmsg.setContent(java.lang.Long.toString(totalUtility));
                myAgent.send(utilmsg);
                
                takeDown();
            }
            else {
//                System.out.println("Unknown/null message received");
                block();
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
        
        System.out.println("Student " + getAID().getName() + " terminating.");
    }
    
}
