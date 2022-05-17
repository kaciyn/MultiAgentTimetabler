package Agents;

import Models.Student;
import Objects.Preference;
import Objects.StudentTimetablePreferences;
import Ontology.Elements.*;
import Ontology.Elements.StudentStatistics;
import Ontology.Elements.SwapProposal;
import Ontology.Elements.TutorialSlot;
import Ontology.Elements.UnwantedTimeslotListing;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    
    private AID aid;
    
    private Student student;
    
    private Long studentId;
    
    private static int highMinimumSwapUtilityGain;
    private static int mediumMinimumSwapUtilityGain;
    private static int lowMinimumSwapUtilityGain;
    
    private static int unwantedSlotUtilityThreshold;
    private static int lowUnwantedSlotUtilityThreshold = Preference.PREFER_NOT.getUtility();
    private static int mediumUnwantedSlotUtilityThreshold = Preference.NO_PREFERENCE.getUtility();
    
    private static int mediumUtilityThreshold;
    private static int highUtilityThreshold;
    
    private static long unwantedSlotCheckPeriod;
    
    private int initialTotalUtility;
    
    private int totalUtility;
    //counts how many messages agent has sent excl. utility informs and registration
    private int messagesSent;
    
    private StudentTimetablePreferences timetablePreferences;
    
    private AID timetablerAgent;
    private AID utilityAgent;
    
    //keeps collection of currently assigned tutorial slots and whether they are locked due to having been proposed for swap
    //<tutorialSlot,Locked>
    private HashMap<TutorialSlot, Boolean> assignedTutorialSlots;
    
    //<listingId,tutorialSlot>
    private HashMap<Long, TutorialSlot> unwantedTutorialsOnOffer;
    
    //tutorialSlot,listingId>
    private HashMap<TutorialSlot, Long> ownAdvertisedTutorials;
    
    //tutorialSlot,listingId>
    private HashMap<Long, TutorialSlot> unconfirmedAcceptedSwapProposalsBySelf;
    
    //minimum utility gain a student will accept on a swap
    private int minimumSwapUtilityGain;
    
    private boolean initialStats;
    private boolean finalStats;
    
    private boolean end;
    
    private long lastSwapTime;
    
    private long noSwapTimeThreshold;
    
    protected void setup()
    {
        lastSwapTime = System.currentTimeMillis();
        
        unwantedTutorialsOnOffer = new HashMap<>();
        ownAdvertisedTutorials = new HashMap<>();
        unconfirmedAcceptedSwapProposalsBySelf = new HashMap<>();
        messagesSent = 0;
        totalUtility = 0;
        initialTotalUtility = 0;
        
        initialStats = true;
        finalStats = false;
        
        end = false;
        
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

// Printout a welcome message
        System.out.println("Hello! Student " + getAID().getName() + " is ready.");
        aid = getAID();
        
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            student = (Student) args[0];
        }
        if (args != null && args.length > 1) {
            highMinimumSwapUtilityGain = (int) args[1];
        }
        if (args != null && args.length > 2) {
            mediumMinimumSwapUtilityGain = (int) args[2];
        }
        if (args != null && args.length > 3) {
            lowMinimumSwapUtilityGain = (int) args[3];
        }
        if (args != null && args.length > 4) {
            mediumUtilityThreshold = (int) args[4];
        }
        if (args != null && args.length > 5) {
            highUtilityThreshold = (int) args[5];
        }
        if (args != null && args.length > 6) {
            unwantedSlotCheckPeriod = (long) args[6];
        }
        if (args != null && args.length > 7) {
            unwantedSlotCheckPeriod = (long) args[7];
        }
        
        assignedTutorialSlots = new HashMap<>();
        timetablePreferences = student.getStudentTimetablePreferences();
        studentId = student.getMatriculationNumber();
        
        addBehaviour(new WakerBehaviour(this, 15000)
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
                
                // Search for utility agent
                DFAgentDescription utilTemplate = new DFAgentDescription();
                var utilSd = new ServiceDescription();
                utilSd.setType("utilityAgent");
                utilTemplate.addServices(utilSd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, utilTemplate);
                    if (result.length > 0) {
                        utilityAgent = result[0].getName();
                    }
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                myAgent.addBehaviour(new UtilityRegistrationServer());
                
                minimumSwapUtilityGain = mediumMinimumSwapUtilityGain;
                
                // Register with timetabler
                myAgent.addBehaviour(new TimetablerRegistration());

//                while (assignedTutorialSlots.size()<1){
//                    doWait();
//                }
                
                //todo this may need to be a wakerbehaviour
                myAgent.addBehaviour(new StatsSender());
                myAgent.addBehaviour(new StatsPollListener());
                
                myAgent.addBehaviour(new EndListener());
                
                myAgent.addBehaviour(new SwapBehaviour());
                
            }
            
        });
    }
    
    //inform timetabler agent it wishes to register
    private class TimetablerRegistration extends OneShotBehaviour
    {
        @Override
        public void action()
        {
            var registration = new ACLMessage(ACLMessage.INFORM);
            registration.addReceiver(timetablerAgent);
            //send matric to timetabler to register
            registration.setContent(java.lang.Long.toString(student.getMatriculationNumber()));
            registration.setConversationId("register");
            
            send(registration);
            
            //receive response
            var mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var reply = blockingReceive(mt);
            
            if (reply != null && reply.getConversationId().equals("register")) {
                try {
                    ContentElement contentElement;

//                    System.out.println(reply.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (contentElement instanceof IsAssignedTo) {
                        var isAssignedTo = (IsAssignedTo) contentElement;
//                        System.out.println(aid.getName() + " received tutorials ");
                        
                        isAssignedTo.getTutorialSlots().forEach(tutorialSlot -> {
                            assignedTutorialSlots.put(tutorialSlot, false);
                        });
                        
                        var tutorialSlots = new ArrayList<TutorialSlot>();
                        tutorialSlots.addAll(assignedTutorialSlots.keySet());
                        
                        totalUtility = timetablePreferences.getTotalUtility(tutorialSlots, timetablePreferences);
                        initialTotalUtility = totalUtility;
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
            //RESPOND TO ADVERTISED SLOTS//
            addSubBehaviour(new UnwantedSlotListReceiver());
            
            addSubBehaviour(new UnavailableSlotListReceiver());
            
            addSubBehaviour(new SwapProposalResultReceiver());
            
            addSubBehaviour(new AcceptedProposalResultReceiver());
            
            //ADVERTISE OWN SLOTS//
            addSubBehaviour(new RequestSwapUnwantedSlots());
            
            addSubBehaviour(new SwapProposalReceiver());
            
            addSubBehaviour(new UnwantedSwapResultReceiver());
            
        }
    }
    
    //PROPOSAL//
    //for RECEIVING unwanted slots
    private class UnwantedSlotListReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender().equals(timetablerAgent) && msg.getConversationId().equals("unwanted-slot")) {
                //receive response
                
                try {
                    ContentElement contentElement = null;

//                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof IsOnOffer) {
                        var isOnOffer = (IsOnOffer) contentElement;
                        
                        var unwantedTimeslotListing = isOnOffer.getUnwantedTimeslotListing();
                        
                        unwantedTutorialsOnOffer.put(unwantedTimeslotListing.getUnwantedListingId(), unwantedTimeslotListing.getTutorialSlot());
                        
                        myAgent.addBehaviour(new SwapOfferProposer(this.myAgent, unwantedSlotCheckPeriod));
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
    
    //notifies that an advertised slot has been taken & removes if from list
    private class UnavailableSlotListReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender().equals(timetablerAgent) && msg.getConversationId().equals("taken-slot")) {
                //receive response
                
                try {
                    ContentElement contentElement = null;

//                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof IsNoLongerOnOffer) {
                        var isNoLongerOnOffer = (IsNoLongerOnOffer) contentElement;
                        
                        unwantedTutorialsOnOffer.remove(isNoLongerOnOffer.getUnwantedTimeslotListing().getUnwantedListingId());
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
    
    //makes an offer to swap own slot
    //maybe turn into a ticker
    private class SwapOfferProposer extends TickerBehaviour
    {
        
        public SwapOfferProposer(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            
            var timetablePreferences = student.getStudentTimetablePreferences();
            
            //updates strategy if necessary
            AdjustStrategy();
            
            if (unwantedTutorialsOnOffer != null) {
                assignedTutorialSlots.forEach((currentTutorialSlot, isLocked) -> {
                    //could also obviously change this so it goes through all available available slots to optimise offers but likely this would slow both the agent and the system down too much
                    
                    var module = currentTutorialSlot.getModuleId();
                    
                    //TODO CHECK THIS WORKS AS EXPECTED
                    //filters slots on offer for tutorials of the same module as current tutorial slot
//                    var unwantedTutorialsOnOfferFiltered = unwantedTutorialsOnOffer.entrySet()
//                                                                                   .stream()
//                                                                                   .filter(element -> currentTutorialSlot.getModuleId().equals(element.getValue().getModuleId()))
//                                                                                   .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
//
                    var unwantedTutorialsOnOfferFiltered = unwantedTutorialsOnOffer.entrySet()
                                                                                   .stream()
                                                                                   .filter(element -> module.equals(element.getValue().getModuleId()))
                                                                                   .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
                    var tlist = unwantedTutorialsOnOfferFiltered.entrySet();

//                    unwantedTutorialsOnOfferFiltered.forEach((l,t)->{
//                        if (t.getModuleId()!=module){
//                            var sdf=4453;
//                        }
//                    });
                    
                    if (unwantedTutorialsOnOfferFiltered.size() > 0) {
                        Long bestSwapListingId = null;
                        //sets min. threshold for utility change
                        var bestUtilityChange = minimumSwapUtilityGain;
                        
                        // Iterating HashMap through for loop
                        for (Map.Entry<Long, TutorialSlot> set :
                                unwantedTutorialsOnOfferFiltered.entrySet()) {
                            
                            var tutorialSlot = set.getValue();
                            var listingId = set.getKey();
                            
                            var proposedUtility = timetablePreferences.getTimeslotUtility(tutorialSlot.getTimeslotId());
                            var utilityChange = proposedUtility - timetablePreferences.getTimeslotUtility(currentTutorialSlot.getTimeslotId());
                            
                            if (utilityChange >= bestUtilityChange) {
                                bestSwapListingId = listingId;
                                bestUtilityChange = utilityChange;
                            }
                        }
                        //CHECK IF LOCKED IF YES THEN SEND A REQUEST TO MAKE UNAVAILABLE AND UNLOCK
                        //IN SWAP PROPOSAL RECEIVER CHECK IF THE SLOT IS UNLOCKED AND IF IT IS REJECT ALL OFFER
                        //could add an extra condition here to only do it if it's a Really good increase+save the second best utility change and propose that instead if it doesn't meet the threshold
                        if (bestSwapListingId != null) {
                            if (isLocked) {
                                //unlocks slot, all swap offers for slot will now be rejected
                                //hopefully this will not cause mischief
                                assignedTutorialSlots.put(currentTutorialSlot, false);
                                
                                //requests the slot be delisted
                                var message = new ACLMessage(ACLMessage.REQUEST);
                                message.addReceiver(timetablerAgent);
                                
                                message.setLanguage(codec.getName());
                                message.setOntology(ontology.getName());
                                message.setConversationId("delist-advertised-slot");
                                
                                var unwantedSlotListingId = ownAdvertisedTutorials.get(currentTutorialSlot);
                                var unwantedListingToDelist = new UnwantedTimeslotListing();
                                
                                unwantedListingToDelist.setUnwantedListingId(unwantedSlotListingId);
                                unwantedListingToDelist.setTutorialSlot(currentTutorialSlot);
                                
                                var delistAdvertisedSlot = new DelistUnwantedSlot();
                                delistAdvertisedSlot.setSlotToDelist(unwantedListingToDelist);
                                
                                var delist = new Action();
                                delist.setAction(delistAdvertisedSlot);
                                delist.setActor(timetablerAgent); // the agent that you request to perform the action
                                
                                var agreed = false;
                                var count = 0;
                                while (!agreed && count <= 5) {
                                    try {
                                        getContentManager().fillContent(message, delist);
                                        send(message);
                                    }
                                    catch (Codec.CodecException e) {
                                        e.printStackTrace();
                                    }
                                    catch (OntologyException e) {
                                        e.printStackTrace();
                                    }
                                    
                                    //receive response
                                    var replyTemplate = MessageTemplate.and(
                                            MessageTemplate.MatchSender(timetablerAgent),
                                            MessageTemplate.MatchConversationId("delist-advertised-slot"));
                                    var reply = myAgent.receive(replyTemplate);
                                    if (reply != null) {
                                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                                            agreed = true;
                                        }
                                    }
                                    else {
                                        block();
                                    }
                                    count++;
                                    //todo watch out for stuckness here
                                }
                                
                            }
                            
                            // Prepare the action request message
                            var message = new ACLMessage(ACLMessage.PROPOSE);
                            message.addReceiver(timetablerAgent);
                            //slightly truncated contract net
                            message.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                            message.setLanguage(codec.getName());
                            message.setOntology(ontology.getName());
                            message.setConversationId("offer-timeslot-swap");
                            
                            var swapProposal = new SwapProposal();
                            swapProposal.setUnwantedListingId(bestSwapListingId);
                            swapProposal.setProposedSlot(currentTutorialSlot);
                            
                            var proposeSwap = new ProposeSwapToTimetabler();
                            proposeSwap.setSwapProposal(swapProposal);
                            proposeSwap.setProposer(aid);
                            
                            //locks tutorial so it isn't proposed somewhere else
                            assignedTutorialSlots.put(currentTutorialSlot, true);
                            
                            var offer = new Action();
                            offer.setAction(proposeSwap);
                            //actor here is timetabler since it's essentially forwarding on the proposal
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
    }
    
    //is waiting for the reply in the SwapOfferProposer enough or does it need a separate listener? it's a one shot so it spins up a new one for each offer so maybe it isn't necessary?
    
    //receives result of own swap proposal (i.e. accept/reject)
    private class SwapProposalResultReceiver extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            //receive response
            var mt = MessageTemplate.and(MessageTemplate.and(MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL), MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)),
                                                             MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET)), MessageTemplate.and(
                    MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("propose-timeslot-swap")));
            
            var reply = myAgent.receive(mt);
            
            if (reply != null) {
                
                ContentElement contentElement = null;

//                System.out.println(reply.getContent()); //print out the message content in SL
                
                // Let JADE convert from String to Java objects
                // Output will be a ContentElement
                try {
                    
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (contentElement instanceof ProposeSwapToTimetabler) {
                        var proposeSwapToTimetabler = (ProposeSwapToTimetabler) contentElement;
                        if (proposeSwapToTimetabler.getProposer().equals(aid)) {
                            var swapProposal = proposeSwapToTimetabler.getSwapProposal();
                            var proposalId = swapProposal.getProposalId();
                            var proposedSlot = swapProposal.getProposedSlot();
                            
                            var requestedSlotListingId = swapProposal.getUnwantedListingId();
                            var requestedSlot = unwantedTutorialsOnOffer.get(requestedSlotListingId);
                            
                            if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                                
                                System.out.println(student.getMatriculationNumber() + "'s proposal has been accepted: " + proposalId);

//                                if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && acceptedProposal) {

//                                    //removes proposed tutorial and adds new tutorial
//                                    if (proposedSlot.getModuleId() != requestedSlot.getModuleId()) {
//                                        throw new Exception("TUTORIAL SWAP MODULE MISMATCH");
//                                        //this is terrible handling but also really shouldn't happen there's multiple checks for it
//                                    }
                            
                            }
                            else if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                                //unlocks tutorial slot
                                assignedTutorialSlots.put(proposedSlot, false);
                                System.out.println(student.getMatriculationNumber() + "'s proposal has been rejected: " + proposalId);
                            }
                            else {
                                throw new Exception("PERFORMATIVE AND RESULT MISMATCH");
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
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                block();
            }
        }
    }
    
    //receives swap result of confirmed swap proposals
    private class AcceptedProposalResultReceiver extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            var resultInformTemplate = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                               MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET)), MessageTemplate.and(
                    MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("propose-timeslot-swap")));
            
            var swapResultMsg = myAgent.receive(resultInformTemplate);
            
            if (swapResultMsg != null) {
                try {
                    ContentElement contentElement = null;

//                    System.out.println(swapResultMsg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    try {
                        contentElement = getContentManager().extractContent(swapResultMsg);
                        if (contentElement instanceof IsSwapResult) {
                            var isSwapResult = (IsSwapResult) contentElement;
                            var swapProposal = isSwapResult.getSwapProposal();
                            
                            var proposalId = swapProposal.getProposalId();
                            var proposedSlot = swapProposal.getProposedSlot();
                            
                            var unwantedSlotListingId = swapProposal.getUnwantedListingId();
                            var unwantedSlot = unwantedTutorialsOnOffer.get(unwantedSlotListingId);
                            
                            //checks if you're the proposer and for the correct cid
                            if (swapResultMsg.getConversationId().equals("propose-timeslot-swap") && assignedTutorialSlots.get(swapProposal.getProposedSlot())) {
                                if (swapResultMsg.getPerformative() == ACLMessage.INFORM && isSwapResult.isAccepted()) {
                                    var oldUtility = timetablePreferences.getTotalUtility((ArrayList<TutorialSlot>) assignedTutorialSlots.keySet(), timetablePreferences);
                                    
                                    //removes proposed tutorial and adds prev. unwanted tutorial
                                    assignedTutorialSlots.remove(proposedSlot);
                                    assignedTutorialSlots.put(unwantedSlot, false);
                                    
                                    //removes offer (though also will get notice from timetabler)
                                    unwantedTutorialsOnOffer.remove(unwantedSlotListingId);
                                    
                                    //again not really used but ho hey
                                    student.removeTutorialSlot(proposedSlot);
                                    student.addTutorialSlot(unwantedSlot);
                                    
                                    totalUtility = timetablePreferences.getTotalUtility(student.getTutorialSlots(), timetablePreferences);
                                    noSwapTimeThreshold = System.currentTimeMillis();
                                    System.out.println(student.getMatriculationNumber() + "'s utility has changed by: " + (totalUtility - oldUtility));
                                    
                                }
                            }
                            else {
                                assignedTutorialSlots.put(proposedSlot, false);
                                
                                System.out.println("Something went wrong with " + student.getMatriculationNumber() + "'s proposal acceptance confirmation for slot listing: " + unwantedSlotListingId + ", unlocking proposed slot");
                                
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
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                block();
            }
        }
    }
    
    //REQUEST/UNWANTED//
    private class SwapProposalReceiver extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            //receive response
            var mt = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchSender(timetablerAgent));
            
            var proposal = myAgent.receive(mt);
            
            if (proposal != null && proposal.getConversationId().equals("propose-timeslot-swap")) {
                try {
                    ContentElement contentElement;

//                    System.out.println(proposal.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(proposal);
                    
                    if (contentElement instanceof Action) {
                        var action = ((Action) contentElement).getAction();
                        if (action instanceof ProposeSwapToStudent) {
                            var proposeSwapToStudent = (ProposeSwapToStudent) action;
                            
                            var swapProposal = proposeSwapToStudent.getSwapProposal();
                            var proposalReply = proposal.createReply();
                            //surely it's ok to eschew the action model when it's just an accept/reject answer
                            if (unconfirmedAcceptedSwapProposalsBySelf.containsKey(swapProposal.getUnwantedListingId())) {
                                
                                proposalReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                System.out.println(student.getMatriculationNumber() + " rejected proposal to swap slot for " + swapProposal.getUnwantedListingId() + "; already accepted a different offer");
                            }
                            else {
                                var proposedTutorialSlot = swapProposal.getProposedSlot();
                                var unwantedTutorialSlot = unwantedTutorialsOnOffer.get(swapProposal.getUnwantedListingId());
                                
                                var proposedUtility = timetablePreferences.getTimeslotUtility(proposedTutorialSlot.getTimeslotId());
                                var utilityChange = proposedUtility - timetablePreferences.getTimeslotUtility(unwantedTutorialSlot.getTimeslotId());
                                
                                getContentManager().fillContent(proposalReply, proposeSwapToStudent);
                                
                                //checks if the slot has not been unlocked/made unavailable in meantime OR if the utility change is under threshold -> rejects
                                if (!assignedTutorialSlots.get(unwantedTutorialSlot) || utilityChange <= minimumSwapUtilityGain) {
                                    proposalReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                    System.out.println(student.getMatriculationNumber() + " rejected proposal to swap slot for " + proposedTutorialSlot);
                                    
                                }
                                else {
                                    proposalReply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    System.out.println(student.getMatriculationNumber() + "accepted proposal to swap slot for " + proposedTutorialSlot);
                                    
                                    //moves advertised tutorial into waiting room
                                    unconfirmedAcceptedSwapProposalsBySelf.put(swapProposal.getUnwantedListingId(), unwantedTutorialSlot);
                                    ownAdvertisedTutorials.remove(unwantedTutorialSlot);
                                    
                                    send(proposalReply);
                                    messagesSent++;
                                    
                                    //should wait for confirmation to do any swaps
                                    
                                }
                                
                            }
                            send(proposalReply);
                            messagesSent++;
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
            else {
                block();
            }
        }
    }
    
    public class UnwantedSwapResultReceiver extends CyclicBehaviour
    {
        //todo i'd watch out here in case it manages to get it in before it's marked as confirmed somehow
        @Override
        public void action() {
            //receive response
            var confirmTemplate = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                          MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET)), MessageTemplate.and(
                    MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("propose-timeslot-swap")));
            
            var swapResultMsg = myAgent.receive(confirmTemplate);
            
            if (swapResultMsg != null) {
                ContentElement contentElement = null;

//                System.out.println(swapResultMsg.getContent()); //print out the message content in SL
                
                // Let JADE convert from String to Java objects
                // Output will be a ContentElement
                try {
                    contentElement = getContentManager().extractContent(swapResultMsg);
                    if (contentElement instanceof IsSwapResult) {
                        var isSwapResult = (IsSwapResult) contentElement;
                        var swapProposal = isSwapResult.getSwapProposal();
                        
                        var proposalId = swapProposal.getProposalId();
                        var proposedSlot = swapProposal.getProposedSlot();
                        
                        var unwantedSlotListingId = swapProposal.getUnwantedListingId();
                        var unwantedSlot = unwantedTutorialsOnOffer.get(unwantedSlotListingId);
                        
                        if (swapResultMsg.getConversationId().equals("propose-timeslot-swap")) {
                            if (swapResultMsg.getPerformative() == ACLMessage.INFORM && isSwapResult.isAccepted()) {
                                var oldUtility = timetablePreferences.getTotalUtility(student.getTutorialSlots(), timetablePreferences);
                                
                                //removes unwanted tutorial and adds new tutorial
                                assignedTutorialSlots.remove(unwantedSlot);
                                assignedTutorialSlots.put(proposedSlot, false);
                                
                                unconfirmedAcceptedSwapProposalsBySelf.remove(swapProposal.getUnwantedListingId());
                                
                                //again not really used but ho hey
                                student.removeTutorialSlot(unwantedSlot);
                                student.addTutorialSlot(proposedSlot);
                                
                                totalUtility = timetablePreferences.getTotalUtility(student.getTutorialSlots(), timetablePreferences);
                                
                                lastSwapTime = System.currentTimeMillis();
                                
                                System.out.println(student.getMatriculationNumber() + "'s utility has changed by: " + (totalUtility - oldUtility));
                                
                            }
                        }
                        else {
                            ownAdvertisedTutorials.put(unwantedSlot, unwantedSlotListingId);
                            unconfirmedAcceptedSwapProposalsBySelf.remove(unwantedSlotListingId, unwantedSlot);
                            System.out.println("Something went wrong with " + student.getMatriculationNumber() + "'s proposal acceptance confirmation for slot listing: " + unwantedSlotListingId);
                            
                        }
                        unconfirmedAcceptedSwapProposalsBySelf.remove(swapProposal.getUnwantedListingId());
                        
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
    
    //send unwanted slots to be advertised
    public class RequestSwapUnwantedSlots extends ParallelBehaviour
    {
        public RequestSwapUnwantedSlots() {
            super(ParallelBehaviour.WHEN_ALL);
        }
        
        //requests unwanted slots be listed for offers
//    private class ListUnwantedSlotRequester extends Behaviour
        
        @Override
        public void onStart() {
            //this is only a oneshot initially because it gets triggered when assigned tutorials or the strategy changes
            addSubBehaviour(new ListUnwantedSlotRequestSender(this.myAgent, unwantedSlotCheckPeriod));
            addSubBehaviour(new ListUnwantedSlotRequestConfirmationReceiver());
            
        }
    }
    
    public class ListUnwantedSlotRequestSender extends TickerBehaviour
    {
        public ListUnwantedSlotRequestSender(Agent a, long period) {
            super(a, period);
        }
        //this would be for per-agent utility threshold termination
//        boolean finished = false;
        
        @Override
        protected void onTick() {
            AdjustStrategy();
            CheckConstraints();

//            if (totalUtility < utilityThreshold) {
            assignedTutorialSlots.forEach((tutorialSlot, isLocked) -> {
                
                var timeslotUtility = timetablePreferences.getTimeslotUtility(tutorialSlot.getTimeslotId());
                if (timeslotUtility <= unwantedSlotUtilityThreshold && !isLocked) {
                    
                    var listUnwantedSlot = new ListUnwantedSlot();
                    
                    listUnwantedSlot.setUnwantedTutorialSlot(tutorialSlot);
                    listUnwantedSlot.setRequestingStudentAgent(aid);
                    
                    //old notes:
                    // issue with this is that it's offering up all the negative utility slots right off the bat which doesn't leave any to offer up
                    // maybe undesired slots from the same module can be automatically swapped out by timetabler agent? no we need to check that the other slot also isn't undesired - just make option to retract offer?
                    
                    var request = new ACLMessage(ACLMessage.REQUEST);
                    request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    
                    request.setLanguage(codec.getName());
                    request.setOntology(ontology.getName());
                    request.addReceiver(timetablerAgent);
                    request.setConversationId("list-unwanted-slot");
                    
                    var listAdvertisedSlot = new ListUnwantedSlot();
                    
                    listAdvertisedSlot.setRequestingStudentAgent(aid);
                    listAdvertisedSlot.setUnwantedTutorialSlot(tutorialSlot);
                    
                    var list = new Action();
                    list.setAction(listAdvertisedSlot);
                    list.setActor(timetablerAgent); // the agent that you request to perform the action
                    
                    try {
                        // Let JADE convert from Java objects to string
                        getContentManager().fillContent(request, list);
                        send(request);
                        messagesSent++;
                        
                        System.out.println(aid.getName() + " sent unwanted tutorial listing request request for:" + tutorialSlot.getTimeslotId());
                        
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
                    
                    //hopefully???? the strategy adjustment will help?
                    // vv something along these lines so we don't go swapping it away
                    // WOULD BE NICE TO IMPLEMENT THE OPTION TO RESCIND OFFER AND STICK IT BACK INTO THE POOL AFTER TIMEOUT actually this may be necessary to stop the system from getting stuck - a waker behaviour?
                }
            });
//            }
//            else {
//                finished = true;
//            }
        
        }
        
    }
    
    public class ListUnwantedSlotRequestConfirmationReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            //receive response
            var mt = MessageTemplate.and(
                    //todo not sure if tagging the protocol here is deserved due to truncation of protocol
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.and(MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("list-unwanted-slot")));
            
            var reply = myAgent.receive(mt);
            
            ContentElement contentElement;
            
            try {
                if (reply != null && reply.getConversationId().equals("list-unwanted-slot")) {
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (contentElement instanceof Predicate) {
                        var predicate = ((Predicate) contentElement);
                        
                        if (predicate instanceof IsListed) {
                            var isListed = (IsListed) predicate;
                            var unwantedTimeslotListing = isListed.getUnwantedTimeslotListing();
                            var unwantedTimeslot = unwantedTimeslotListing.getTutorialSlot();
                            if (reply.getPerformative() == ACLMessage.INFORM) {
                                System.out.println(aid.getName() + "'s unwanted tutorial listed: " + unwantedTimeslot);
                                
                                ownAdvertisedTutorials.put(unwantedTimeslot, unwantedTimeslotListing.getUnwantedListingId());
                                
                                //locks slot
                                assignedTutorialSlots.put(unwantedTimeslot, true);
                            }
                            else {
                                System.out.println(aid.getName() + "'s unwanted tutorial not listed, unlocking slot " + unwantedTimeslot.getTimeslotId());
                                
                                //unlocks slot so offer can be repeated
                                assignedTutorialSlots.put(unwantedTimeslot, false);
                            }
                            
                        }
                    }
                }
                else {
                    block();
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
    
    //UTILITY//
    private class UtilityRegistrationServer extends OneShotBehaviour
    {
        @Override
        public void action()
        {
            ACLMessage registration = new ACLMessage(ACLMessage.INFORM);
            registration.addReceiver(utilityAgent);
            //send matric to utilityAgent to register
            registration.setConversationId("register-utility");
            
            //again, poor handling
//            var registered = false;
//            while (!registered) {
            send(registration);
            
            //receive response
            var mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
            var reply = myAgent.receive(mt);
            
            if (reply != null && reply.getConversationId().equals("register-utility")) {
                System.out.println(aid + " registered with UtilityAgent");
                
            }
            else {
                block();
            }
        }
    }
    
    //listens for poll and sends utility & messagesSent
    private class StatsPollListener extends CyclicBehaviour
    {
        @Override
        public void action() {
            
            var mt = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchSender(utilityAgent),
                                                             MessageTemplate.MatchPerformative(ACLMessage.REQUEST)), MessageTemplate.MatchConversationId("current-stats"));
            
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender().equals(utilityAgent)) {
                myAgent.addBehaviour(new StatsSender());
                
            }
            else {
                block();
            }
        }
    }
    
    //listens for poll and sends utility & messagesSent
    private class StatsSender extends OneShotBehaviour
    {
        @Override
        public void action() {
            
            var msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(utilityAgent);
            //send matric to utilityAgent to register
            msg.setConversationId("current-stats");
            msg.setOntology(ontology.getName());
            msg.setLanguage(codec.getName());
            msg.setOntology(ontology.getName());
            
            var areCurrentFor = new AreCurrentFor();
            var studentStats = new StudentStatistics();
            studentStats.setCurrentTotalUtility((long) totalUtility);
            studentStats.setMessagesSent((long) messagesSent);
            studentStats.setInitialStats(initialStats);
            studentStats.setFinalStats(finalStats);
            
            areCurrentFor.setStudent(myAgent.getAID());
            areCurrentFor.setStudentStats(studentStats);
            
            try {
                getContentManager().fillContent(msg, areCurrentFor);
                send(msg);
            }
            catch (Codec.CodecException e) {
                e.printStackTrace();
            }
            catch (OntologyException e) {
                e.printStackTrace();
            }
            
            if (initialStats) {
                initialStats = false;
            }
            
        }
    }
    
    private class EndListener extends CyclicBehaviour
    {
        
        @Override
        public void action() {
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("end"));
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender().equals(utilityAgent)) {
                end = true;
                //sends final stats to utility agent
                finalStats = true;
                myAgent.addBehaviour(new StatsSender());
                
                System.out.println("Student " + student.getMatriculationNumber() + " total utility achieved:" + totalUtility + " with: " + messagesSent + " messages sent.");
                
                var takedownConfirmMsg = new ACLMessage(ACLMessage.INFORM);
                takedownConfirmMsg.addReceiver(utilityAgent);
                
                takedownConfirmMsg.setConversationId("ending");
                
                takedownConfirmMsg.setContent("Shutting down " + myAgent.getAID());
                send(takedownConfirmMsg);
                
                takeDown();
            }
            else {
//                System.out.println("Unknown/null message received");
                block();
            }
            
        }
        
    }
    
    //updates strategy according to current state
    //todo could also have the utility overlord meddle here and command/request the agent to laxen the strategy if the global utility isn't looking good or is rising too slowly; could actually target local maxima to make them less selfish but that is OUTWITH THIS PROJECT AND MY TIME AND ABILITIES
    public void AdjustStrategy() {
        //set utility threshold low for very low utility gains, seems counterintuitive but it gives more dynamism possibly to get out of the hole
        
        if (totalUtility >= highUtilityThreshold) {
            //geared at making small gains to already a good threshold
            unwantedSlotUtilityThreshold = mediumUnwantedSlotUtilityThreshold;
            minimumSwapUtilityGain = lowMinimumSwapUtilityGain;
        }
        else {
            if (totalUtility >= mediumUtilityThreshold && totalUtility < highUtilityThreshold) {
                unwantedSlotUtilityThreshold = lowUnwantedSlotUtilityThreshold;
                minimumSwapUtilityGain = mediumMinimumSwapUtilityGain;
                
            }
            if (totalUtility < mediumUtilityThreshold) {
                unwantedSlotUtilityThreshold = lowUnwantedSlotUtilityThreshold;
                minimumSwapUtilityGain = lowMinimumSwapUtilityGain;
            }
            //attempts to bump agent and system out of a mediocre or bad local optimum
            if ((System.currentTimeMillis() - lastSwapTime) > noSwapTimeThreshold) {
                if (unwantedSlotUtilityThreshold > lowMinimumSwapUtilityGain) {
                    System.out.println("Agent " + aid.getName() + " has reached the NO SWAP THRESHOLD and ACTIVATED THE EXCITING STRATEGY CHANGE at " + (System.currentTimeMillis() - lastSwapTime) / 1000 + " seconds since LAST SWAP");
                }
                unwantedSlotUtilityThreshold = lowUnwantedSlotUtilityThreshold;
                minimumSwapUtilityGain = lowMinimumSwapUtilityGain;
            }
            
        }
        
    }
    
    //by nature of how the initial assignments are done and how they are modified there should implicitly be no constraint violations but here's a test anyway
    public void CheckConstraints() {
        
        ArrayList<String> moduleIds = student.getModuleIds();
        for (int i = 0, moduleIdsSize = moduleIds.size(); i < moduleIdsSize; i++) {
            String moduleId = moduleIds.get(i);
            var count = 0;
            for (Map.Entry<TutorialSlot, Boolean> entry : assignedTutorialSlots.entrySet()) {
                TutorialSlot tutorialSlot = entry.getKey();
                if (tutorialSlot.getModuleId() == moduleId) {
                    count++;
                }
            }
            assert count > 0 : "Module and assignedTutorial mismatch: Enrolled " + moduleId + " has " + count + " corresponding tutorials";
        }
        
        assignedTutorialSlots.forEach((tutorialSlot, aBoolean) -> {
            assert student.getModuleIds().contains(tutorialSlot.getModuleId()) : "Student " + studentId + " assigned to tutorial for module (" + tutorialSlot.getModuleId() + ") they aren't enrolled in, tutorial: " + tutorialSlot.getTimeslotId();
        });
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