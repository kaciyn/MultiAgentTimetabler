package Agents;

import Models.Module;
import Models.Student;
import Objects.Preference;
import Objects.StudentTimetablePreferences;
import Ontology.Elements.*;
import Ontology.Elements.StudentAgentMetrics;
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
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    
    private static long unwantedSlotUtilityThreshold;
    private static long lowUnwantedSlotUtilityThreshold = Preference.PREFER_NOT.getUtility();
    private static long mediumUnwantedSlotUtilityThreshold = Preference.NO_PREFERENCE.getUtility();
    
    private static long mediumUtilityThreshold;
    private static long highUtilityThreshold;
    
    private static long swapEvaluationPeriod;
    
    private long initialTimetableUtility;
    
    private long timetableUtility;
    //counts how many messages agent has sent excl. utility informs and registration
    private int messagesSent;
    
    private StudentTimetablePreferences timetablePreferences;
    
    private AID timetablerAgent;
    private AID utilityAgent;
    
    //keeps collection of currently assigned tutorial slots and whether they are locked due to having been proposed for swap
    //<tutorialSlot,Locked>
    private ConcurrentHashMap<TutorialSlot, Boolean> assignedTutorialSlots;
    
    //<listingId,tutorialSlot>
    private ConcurrentHashMap<Long, TutorialSlot> unwantedTutorialsOnOffer;
    
    //tutorialSlot,listingId>
    private ConcurrentHashMap<TutorialSlot, Long> ownCFPedTutorialSlots;
    
    //listingId,ProposedSwapOffers
    private ConcurrentHashMap<Long, ArrayList<SwapProposal>> unwantedListingSwapProposals;
    
    //listingId,accepted swapproposal>
    private ConcurrentHashMap<Long, SwapProposal> unconfirmedAcceptedSwapProposalsBySelf;
    
    //minimum utility gain a student will accept on a swap
    private int minimumSwapUtilityGain;
    
    private boolean initialMetrics;
    private boolean finalMetrics;
    
    private ArrayList<Module> modules;
    
    private boolean end;
    private long lastSwapTime;
    
    private long noSwapTimeThreshold;
    
    private int maximumTimetableUtility;
    
    protected void setup()
    {
        
        unwantedTutorialsOnOffer = new ConcurrentHashMap<>();
        ownCFPedTutorialSlots = new ConcurrentHashMap<>();
        unconfirmedAcceptedSwapProposalsBySelf = new ConcurrentHashMap<>();
        unwantedListingSwapProposals=new ConcurrentHashMap<>();
        messagesSent = 0;
        timetableUtility = 0;
        initialTimetableUtility = 0;
        
        initialMetrics = true;
        finalMetrics = false;
        
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
            swapEvaluationPeriod = (long) args[6];
        }
        if (args != null && args.length > 7) {
            modules = (ArrayList<Module>) args[7];
        }
        if (args != null && args.length > 8) {
            noSwapTimeThreshold = (Long) args[8];
        }
        
        assignedTutorialSlots = new ConcurrentHashMap<>();
        timetablePreferences = student.getStudentTimetablePreferences();
        studentId = student.getMatriculationNumber();
        
        addBehaviour(new WakerBehaviour(this, 20000)
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
                
                minimumSwapUtilityGain = highMinimumSwapUtilityGain;
                
                // Register with timetabler
                addBehaviour(new TimetablerRegistration());

//                addBehaviour(new UtilityRegistrationServer());

//                while (assignedTutorialSlots.size()<1){
//                    doWait();
//                }
                
                //todo this may need to be a wakerbehaviour
                addBehaviour(new MetricsSender());
                addBehaviour(new MetricsPollListener());
                
                addBehaviour(new EndListener());
                
                addBehaviour(new SwapBehaviour());
                
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
            registration.setLanguage(codec.getName());
            registration.setOntology(ontology.getName());
            //send matric to timetabler to register
            registration.setContent(java.lang.Long.toString(student.getMatriculationNumber()));
            registration.setConversationId("register");
            
            send(registration);
            System.out.println(student.getMatriculationNumber() + " sent timetabler registration ");
            
            //receive response
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("register"));
            var reply = blockingReceive(mt);
            
            if (reply != null) {
                try {
                    ContentElement contentElement;

//                    System.out.println(reply.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (contentElement instanceof IsAssignedTo) {
                        var isAssignedTo = (IsAssignedTo) contentElement;
                        System.out.println(aid.getName() + " received tutorials ");
                        
                        isAssignedTo.getTutorialSlots().forEach(tutorialSlot -> {
                            assignedTutorialSlots.put(tutorialSlot, false);
                        });
                        
                        var tutorialSlots = new ArrayList<TutorialSlot>();
                        tutorialSlots.addAll(assignedTutorialSlots.keySet());
                        
                        timetableUtility = timetablePreferences.getTotalUtility(tutorialSlots, timetablePreferences);
                        initialTimetableUtility = timetableUtility;
                        maximumTimetableUtility=timetablePreferences.getOptimalUtility(modules,timetablePreferences);
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
            //RESPOND TO ADVERTISED (REQUESTED) SLOTS//PROPOSE
            addSubBehaviour(new UnwantedSlotListCFPReceiver());
            
            addSubBehaviour(new DelistedSlotListReceiver());
            
            addSubBehaviour(new SwapProposalResponseReceiver());
            
            addSubBehaviour(new AcceptedProposalResultReceiver());
            
            //ADVERTISE(REQUESTED) OWN SLOTS//REQUEST
            addSubBehaviour(new InitiateSwapRequestBehaviours());
            
            addSubBehaviour(new SwapProposalReceiver());
            
            addSubBehaviour(new AcceptedSwapProposalResultReceiver());
            
            lastSwapTime = System.currentTimeMillis();
            
        }
    }
    
    ///////PROPOSE/////
    //PROPOSAL//
    //for RECEIVING unwanted slots
    private class UnwantedSlotListCFPReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt =
                    MessageTemplate.and(
                            MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                            MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                                                MessageTemplate.MatchConversationId("timeslot-swap")));
            
            var msg = myAgent.receive(mt);
            
            if (msg != null) {
                block();
            }
            if (msg.getSender().equals(timetablerAgent)) {
                System.out.println(student.getMatriculationNumber() + " received unwanted slot CFP");
                
                try {
                    ContentElement contentElement;
                    
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof ProposeSwapToTimetabler) {
                        var proposeSwapToTimetabler = (ProposeSwapToTimetabler) contentElement;
                        
                        var unwantedTimeslotListing = proposeSwapToTimetabler.getUnwantedTimeslotListing();
                        
                        unwantedTutorialsOnOffer.put(unwantedTimeslotListing.getUnwantedListingId(), unwantedTimeslotListing.getTutorialSlot());
                        
                        addBehaviour(new EvaluateCurrentSwapCFPs());
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
    
    //notifies that an advertised slot has been delisted & removes if from list
    private class DelistedSlotListReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender().equals(timetablerAgent) && msg.getConversationId().equals("delisted-slot")) {
                //receive response
                System.out.println(student.getMatriculationNumber() + " received delisted slot announcement");
                
                try {
                    ContentElement contentElement;

//                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof IsDelisted) {
                        var isNoLongerOnOffer = (IsDelisted) contentElement;
                        
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
    
    private
    class EvaluateSwapsForTutorial extends OneShotBehaviour
    {
        private TutorialSlot currentTutorialSlot;
        private Boolean isLocked;
        
        public EvaluateSwapsForTutorial(TutorialSlot currentTutorialSlot, Boolean isLocked) {
            
            this.currentTutorialSlot = currentTutorialSlot;
            this.isLocked = isLocked;
        }
        
        @Override
        public void action() {
            System.out.println(student.getMatriculationNumber() + " evaluating swaps for tutorial slot: " + currentTutorialSlot.getTimeslotId());
            
            //filters slots on offer for tutorials of the same module as current tutorial slot
            var unwantedTutorialsOnOfferFiltered = unwantedTutorialsOnOffer.entrySet()
                                                                           .stream()
                                                                           .filter(element -> currentTutorialSlot.getModuleId().equals(element.getValue().getModuleId()))
                                                                           .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
            
            if (unwantedTutorialsOnOfferFiltered.size() <= 0) {
                System.out.println(student.getMatriculationNumber() + " no tutorials of same module found");
                
                return;
            }
            var bestSwapListing = new UnwantedTimeslotListing();
            //sets min. threshold for utility change
            var bestUtilityChange = 0;
            
            var currentTutorialSlotUtility = timetablePreferences.getTimeslotUtility(currentTutorialSlot.getTimeslotId());
            
            // Iterating ConcurrentHashMap of tutorialoffers through for loop
            for (Map.Entry<Long, TutorialSlot> set :
                    unwantedTutorialsOnOfferFiltered.entrySet()) {
                
                var tutorialSlot = set.getValue();
                var listingId = set.getKey();
                
                var proposedUtility = timetablePreferences.getTimeslotUtility(tutorialSlot.getTimeslotId());
                var utilityChange = proposedUtility - currentTutorialSlotUtility;
                
                if (utilityChange >= bestUtilityChange) {
                    
                    bestSwapListing = new UnwantedTimeslotListing(tutorialSlot, listingId);
                    bestUtilityChange = utilityChange;
                }
            }
            //CHECK IF LOCKED IF YES THEN SEND A REQUEST TO MAKE UNAVAILABLE AND UNLOCK
            //IN SWAP PROPOSAL RECEIVER CHECK IF THE SLOT IS UNLOCKED AND IF IT IS REJECT ALL OFFERs
            //checks if the swap meets the minimum utility gain defined but overrides the minimum if own tutorial slot is a CANNOT
            if (bestSwapListing == null && bestUtilityChange >= minimumSwapUtilityGain && currentTutorialSlotUtility > 0) {
                System.out.println(student.getMatriculationNumber() + " no good swaps found");
                
                return;
            }
            System.out.println(student.getMatriculationNumber() + " good swap found for " + currentTutorialSlot);
            
            var proposeSwap = new ProposeSwapToTimetabler(bestSwapListing, currentTutorialSlot, this.myAgent.getAID());
            
            addBehaviour(new SendProposal(proposeSwap));
            
            if (isLocked) {
                addBehaviour(new RequestDelistUnwantedSlotForProposal(currentTutorialSlot, proposeSwap));
                
            }
            
            addBehaviour(new SendProposal(proposeSwap));
        }
    }
    
    private
    class RequestDelistUnwantedSlotForProposal extends OneShotBehaviour
    {
        
        private TutorialSlot currentTutorialSlot;
        private ProposeSwapToTimetabler proposeSwap;
        
        public RequestDelistUnwantedSlotForProposal(TutorialSlot currentTutorialSlot, ProposeSwapToTimetabler proposeSwap) {
            
            this.currentTutorialSlot = currentTutorialSlot;
            this.proposeSwap = proposeSwap;
        }
        
        @Override
        public void action() {
            //unlocks slot, all swap offers for slot will now be rejected
            //hopefully this will not cause mischief
            assignedTutorialSlots.put(currentTutorialSlot, false);
            
            //requests the slot be delisted
            var message = new ACLMessage(ACLMessage.REQUEST);
            message.addReceiver(timetablerAgent);
            message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            message.setLanguage(codec.getName());
            message.setOntology(ontology.getName());
            message.setConversationId("delist-advertised-slot");
            
            var unwantedSlotListingId = ownCFPedTutorialSlots.get(currentTutorialSlot);
            var unwantedListingToDelist = new UnwantedTimeslotListing();
            
            unwantedListingToDelist.setUnwantedListingId(unwantedSlotListingId);
            unwantedListingToDelist.setTutorialSlot(currentTutorialSlot);
            
            var delistAdvertisedSlot = new DelistUnwantedSlot();
            delistAdvertisedSlot.setSlotToDelist(unwantedListingToDelist);
            
            var delistRequest = new Action();
            delistRequest.setAction(delistAdvertisedSlot);
            delistRequest.setActor(timetablerAgent); // the agent that you request to perform the action
            
            //attempt 5 times if failed
            var agreed = false;
            var count = 0;
            while (!agreed && count <= 5) {
                try {
                    getContentManager().fillContent(message, delistRequest);
                    send(message);
                    System.out.println(student.getMatriculationNumber() + "sent delist request ");
                    
                    messagesSent++;
                }
                catch (Codec.CodecException e) {
                    e.printStackTrace();
                }
                catch (OntologyException e) {
                    e.printStackTrace();
                }
                
                //receive response, small enough and maybe infrequent enough to not warrant a separate receiver
                var replyTemplate = MessageTemplate.and(
                        MessageTemplate.MatchSender(timetablerAgent),
                        MessageTemplate.MatchConversationId("delist-advertised-slot"));
                var reply = myAgent.receive(replyTemplate);
                if (reply == null) {
                    //not cyclic so no blocking
//                    block();
                    return;
                }
                else {
                    System.out.println(student.getMatriculationNumber() + " received delist request result");
                    
                    if (reply.getPerformative() == ACLMessage.CONFIRM) {
                        System.out.println(student.getMatriculationNumber() + " delist request  confirmed");
                        
                        agreed = true;
                        
                        addBehaviour(new SendProposal(proposeSwap));
                    }
                    else if (reply.getPerformative() == ACLMessage.REFUSE) {
                        if (reply.getContent() != null) {
                            System.out.println("Delist request refused because: " + reply.getContent()); //print out the message content in SL
                            
                        }
                        
                    }
                }
                count++;
                //todo watch out for stuckness here
            }
        }
    }
    
    //evaluates current CFPs
    private class EvaluateCurrentSwapCFPs extends ParallelBehaviour
    {
        public EvaluateCurrentSwapCFPs() {
            super(ParallelBehaviour.WHEN_ALL);
        }
        
        @Override
        public void onStart() {
            
            //updates strategy if necessary
            AdjustStrategy();
            System.out.println(student.getMatriculationNumber() + " evaluating current swap CFPs");
            
            if (unwantedTutorialsOnOffer == null) {
                System.out.println("unwantedTutorialsOnOffer not initialised");
                
                return;
            }
            assignedTutorialSlots.forEach((currentTutorialSlot, isLocked) -> {
                
                addSubBehaviour(new EvaluateSwapsForTutorial(currentTutorialSlot, isLocked));
                
            });
        }
    }
    
    private
    class SwapProposalResponseInformHandler extends OneShotBehaviour
    {
        public SwapProposalResponseInformHandler(ContentElement response) {
        
        }
        
        @Override
        public void action() {
        
        }
    }
    
    //receives response to own swap proposal
    private class SwapProposalResponseReceiver extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            //receive response
            var mt = MessageTemplate.and(MessageTemplate.and(MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL), MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)),
                                                             MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET)), MessageTemplate.and(
                    MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("timeslot-swap"))));
            
            var response = myAgent.receive(mt);
            
            if (response == null) {
                block();
            }
            try {
                System.out.println(student.getMatriculationNumber() + " received swap proposal response");
                
                var contentElement = getContentManager().extractContent(response);
                if (response.getPerformative() == ACLMessage.INFORM) {
                    addBehaviour(new SwapProposalResponseInformHandler(contentElement));
                }
                
                if (contentElement instanceof ProposeSwapToTimetabler) {
                    var proposeSwapToTimetabler = (ProposeSwapToTimetabler) contentElement;
                    if (proposeSwapToTimetabler.getProposer().equals(aid)) {
                        var unwantedTimeslotListing = proposeSwapToTimetabler.getUnwantedTimeslotListing();
                        var proposedSlot = proposeSwapToTimetabler.getProposedSlot();
                        
                        if (response.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                            //unlocks tutorial slot
                            assignedTutorialSlots.put(proposedSlot, false);
                            System.out.println(student.getMatriculationNumber() + "'s proposal has been rejected: " + proposedSlot.getTimeslotId());
                            return;
                        }
                        else if (response.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            var unwantedSlot = unwantedTutorialsOnOffer.get(unwantedTimeslotListing);
                            
                            System.out.println(student.getMatriculationNumber() + "'s proposal has been accepted: " + unwantedSlot.getTimeslotId());

//
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
        
    }
    
    private class SendProposal extends OneShotBehaviour
    {
        
        private ProposeSwapToTimetabler proposeSwap;
        
        public SendProposal(ProposeSwapToTimetabler proposeSwap) {
            
            this.proposeSwap = proposeSwap;
        }
        
        @Override
        public void action() {
            
            // Prepare the action request message
            var message = new ACLMessage(ACLMessage.PROPOSE);
            message.addReceiver(timetablerAgent);
            //slightly truncated contract net
            //TODO CHECK ABOVE ASSERTION IS TRUE
            message.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            message.setLanguage(codec.getName());
            message.setOntology(ontology.getName());
            message.setConversationId("timeslot-swap");
            
            //locks tutorial so it isn't proposed somewhere else
            assignedTutorialSlots.put(proposeSwap.getProposedSlot(), true);
            
            var propose = new Action();
            propose.setAction(proposeSwap);
            //actor here is timetabler since it's essentially forwarding on the proposal
            propose.setActor(timetablerAgent);
            try {
                // Let JADE convert from Java objects to string
                getContentManager().fillContent(message, propose); //send the wrapper object
                send(message);
                messagesSent++;
                System.out.println(student.getMatriculationNumber() + "sent proposal ");
                
                messagesSent++;
            }
            catch (Codec.CodecException ce) {
                ce.printStackTrace();
            }
            catch (OntologyException oe) {
                oe.printStackTrace();
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
                    MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("timeslot-swap")));
            
            var swapResultMsg = myAgent.receive(resultInformTemplate);
            
            if (swapResultMsg != null) {
                System.out.println(student.getMatriculationNumber() + " received accepted proposal result");
                
                ContentElement contentElement;

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
                        if (swapResultMsg.getConversationId().equals("timeslot-swap") && assignedTutorialSlots.get(swapProposal.getProposedSlot())) {
                            if (swapResultMsg.getPerformative() == ACLMessage.INFORM && isSwapResult.isAccepted()) {
                                var tutorialSlots= Arrays.asList((TutorialSlot[])assignedTutorialSlots.keySet().toArray());
                                var oldUtility = timetablePreferences.getTotalUtility(tutorialSlots, timetablePreferences);
                                System.out.println(student.getMatriculationNumber() + "'s swap proposal accepted");
                                
                                //removes proposed tutorial and adds prev. unwanted tutorial
                                assignedTutorialSlots.remove(proposedSlot);
                                assignedTutorialSlots.put(unwantedSlot, false);
                                
                                //removes offer (though also will get notice from timetabler)
                                unwantedTutorialsOnOffer.remove(unwantedSlotListingId);
                                
                                //again not really used but ho hey
                                student.removeTutorialSlot(proposedSlot);
                                student.addTutorialSlot(unwantedSlot);
                                
                                timetableUtility = timetablePreferences.getTotalUtility(student.getTutorialSlots(), timetablePreferences);
                                
                                System.out.println(student.getMatriculationNumber() + "'s utility has changed by: " + (timetableUtility - oldUtility));
                                
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
            
        }
    }
    
    /////REQUEST/UNWANTED/CFP////
    
    //send unwanted slots to be advertised
    public class InitiateSwapRequestBehaviours extends ParallelBehaviour
    {
        public InitiateSwapRequestBehaviours() {
            super(ParallelBehaviour.WHEN_ALL);
        }
        
        //requests unwanted slots be listed for offers
//    private class ListUnwantedSlotRequester extends Behaviour
        
        @Override
        public void onStart() {
            addSubBehaviour(new UnwantedSlotFinder(this.myAgent, swapEvaluationPeriod));
            addSubBehaviour(new ListUnwantedSlotRequestConfirmationReceiver());
            addSubBehaviour(new SwapProposalReceiver());
            addSubBehaviour(new EvaluateCurrentSwapProposals(this.myAgent, swapEvaluationPeriod));
            addSubBehaviour(new AcceptedSwapProposalResultReceiver());
            
        }
    }
    
    public class UnwantedSlotFinder extends TickerBehaviour
    {
        public UnwantedSlotFinder(Agent a, long period) {
            super(a, period);
        }
        //this would be for per-agent utility threshold termination
//        boolean finished = false;
        
        @Override
        protected void onTick() {
            AdjustStrategy();
            CheckConstraints();

//            if (timetableUtility < utilityThreshold) {
            //if own slots was likely to be a bigger collection would be worth parallelising but as it is there's no need
            System.out.println(student.getMatriculationNumber() + " looking for unwanted own slots");
            
            assignedTutorialSlots.forEach((tutorialSlot, isLocked) -> {
                if (timetablePreferences.getTimeslotUtility(tutorialSlot.getTimeslotId()) <= unwantedSlotUtilityThreshold && !isLocked) {
                    System.out.println(student.getMatriculationNumber() + " unwanted slot found");
                    
                    addBehaviour(new ListUnwantedSlotRequestSender(tutorialSlot));
                    
                }
                
            });
        }
        
    }
    
    public class ListUnwantedSlotRequestSender extends OneShotBehaviour
    {
        
        private TutorialSlot unwantedSlot;
        
        public ListUnwantedSlotRequestSender(TutorialSlot unwantedSlot) {
            
            this.unwantedSlot = unwantedSlot;
        }
        
        @Override
        public void action() {
            
            var listUnwantedSlot = new ListUnwantedSlot();
            
            listUnwantedSlot.setUnwantedTutorialSlot(unwantedSlot);
            listUnwantedSlot.setRequestingStudentAgent(aid);
            
            var request = new ACLMessage(ACLMessage.REQUEST);
            request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            
            request.setLanguage(codec.getName());
            request.setOntology(ontology.getName());
            request.addReceiver(timetablerAgent);
            request.setConversationId("timeslot-swap");
            
            var requestedAction = new Action();
            requestedAction.setAction(listUnwantedSlot);
            requestedAction.setActor(timetablerAgent); // the agent that you request to perform the action
            
            try {
                // Let JADE convert from Java objects to string
                getContentManager().fillContent(request, requestedAction);
                send(request);
                System.out.println(aid.getName() + " sent unwanted tutorial listing request for:" + unwantedSlot.getTimeslotId());
                
                messagesSent++;
                
                //puts hold on slot unless the request is rejected
                assignedTutorialSlots.put(unwantedSlot, true);
                
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
    }
    
    //again might be worth parallelising if we were making more slot offers but we are not
    public class ListUnwantedSlotRequestConfirmationReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            //receive response
            var mt = MessageTemplate.and(
                    //todo not sure if tagging the protocol here is deserved due to truncation of protocol
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.and(MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("timeslot-swap")));
            
            var reply = myAgent.receive(mt);
            
            ContentElement contentElement;
            
            try {
                if (reply == null || !reply.getConversationId().equals("timeslot-swap")) {
                    block();
                    return;
                }
                else {
                    System.out.println(student.getMatriculationNumber() + " received list unwanted slot request confirmation message");
                    
                    contentElement = getContentManager().extractContent(reply);
                    
                    if (!(contentElement instanceof Predicate)) {
                        System.out.println("Not a predicate!");
                        return;
                    }
                    var predicate = ((Predicate) contentElement);
                    
                    if (!(predicate instanceof IsListedFor)) {
                        System.out.println("Not the right predicate!!");
                        
                        return;
                    }
                    var isListedFor = (IsListedFor) predicate;
                    if (!isListedFor.getStudent().equals(aid)) {
                        System.out.println("Not FOR me!!!");
                        return;
                        
                    }
                    var unwantedTimeslotListing = isListedFor.getUnwantedTimeslotListing();
                    var unwantedTimeslot = unwantedTimeslotListing.getTutorialSlot();
                    if (reply.getPerformative() == ACLMessage.CONFIRM) {
                        System.out.println(aid.getName() + "'s unwanted tutorial listed: " + unwantedTimeslot);
                        
                        ownCFPedTutorialSlots.put(unwantedTimeslot, unwantedTimeslotListing.getUnwantedListingId());
                        
                        //ensures slot is locked
                        assignedTutorialSlots.put(unwantedTimeslot, true);
                    }
                    else {
                        System.out.println(aid.getName() + "'s unwanted tutorial not listed, unlocking slot " + unwantedTimeslot.getTimeslotId());
                        
                        //unlocks slot so offer can be repeated
                        assignedTutorialSlots.put(unwantedTimeslot, false);
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
    
    private class SwapProposalReceiver extends CyclicBehaviour
    {
        @Override
        public void action()
        {
            //receive response
            MessageTemplate mt =
                    MessageTemplate.and(
                            MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                            MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                                                MessageTemplate.MatchConversationId("timeslot-swap")));
            
            var proposalMessage = myAgent.receive(mt);
            
            if (proposalMessage != null) {
                System.out.println(student.getMatriculationNumber() + " received swap proposal message");
                
                addBehaviour(new SwapProposalHandler(proposalMessage));
                
            }
            else {
                block();
            }
        }
    }
    
    private class SwapProposalHandler extends OneShotBehaviour
    {
        private ACLMessage proposalMessage;
        
        public SwapProposalHandler(ACLMessage proposalMessage) {
            
            this.proposalMessage = proposalMessage;
        }
        
        @Override
        public void action() {
            ContentElement contentElement;
            
            try {

//                    System.out.println(proposal.getContent()); //print out the message content in SL
                
                // Let JADE convert from String to Java objects
                // Output will be a ContentElement
                contentElement = getContentManager().extractContent(proposalMessage);
                
                if (contentElement instanceof Action) {
                    var action = ((Action) contentElement).getAction();
                    if (action instanceof ProposeSwapToStudent) {
                        var proposeSwapToStudent = (ProposeSwapToStudent) action;
                        
                        var swapProposal = proposeSwapToStudent.getSwapProposal();
                        
                        var unwantedTutorialSlotId = unwantedTutorialsOnOffer.get(swapProposal.getUnwantedListingId());
                        
                        var proposalReply = proposalMessage.createReply();
                        
                        if (unwantedListingSwapProposals.get(unwantedTutorialSlotId) != null && !unconfirmedAcceptedSwapProposalsBySelf.containsKey(swapProposal.getUnwantedListingId())) {
                            unwantedListingSwapProposals.get(unwantedTutorialSlotId).add(swapProposal);
                            System.out.println(student.getMatriculationNumber() + " added swap proposal " + swapProposal.getUnwantedListingId());
                            
                        }
                        else {
                            
                            //surely it's ok to eschew the action model when it's just an accept/reject answer
                            proposalReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            System.out.println(student.getMatriculationNumber() + " rejected proposal to swap slot for " + swapProposal.getUnwantedListingId() + "; already accepted a different offer");
                        }
                        
                    }
                    
                }
            }
            catch (OntologyException e) {
                throw new RuntimeException(e);
            }
            catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private class EvaluateCurrentSwapProposals extends TickerBehaviour
    {
        
        public EvaluateCurrentSwapProposals(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            addBehaviour(new EvaluateSwapProposals());
            
        }
    }
    
    private class EvaluateSwapProposals extends ParallelBehaviour
    {
        public EvaluateSwapProposals() {
            super((ParallelBehaviour.WHEN_ALL));
        }
        
        @Override
        public void onStart() {
            unwantedListingSwapProposals.forEach((listingId, swapProposals) -> {
                addSubBehaviour(new EvaluateAndRespondToSwapProposalsForListing(listingId));
            });
        }
        
    }
    
    private class EvaluateAndRespondToSwapProposalsForListing extends OneShotBehaviour
    {
        
        private Long listingId;
        
        public EvaluateAndRespondToSwapProposalsForListing(Long listingId) {
            
            this.listingId = listingId;
        }
        
        @Override
        public void action() {
            
            if (unconfirmedAcceptedSwapProposalsBySelf.containsKey(listingId)) {
                System.out.println(student.getMatriculationNumber() + "'s listing " + listingId + "; has already accepted a proposal, pending confirmation");
                return;
            }
            else {
                var bestSwapProposal = new SwapProposal();
                //sets min. threshold for utility change
                var bestUtilityChange = 0;
                
                var unwantedTutorialSlot = unwantedTutorialsOnOffer.get(listingId);
                var currentTutorialSlotUtility = timetablePreferences.getTimeslotUtility(unwantedTutorialSlot.getTimeslotId());
                
                var swapProposalsForListing = unwantedListingSwapProposals.get(listingId);
                
                //iterates backwards to give precedence to first come proposals
                ListIterator<SwapProposal> swapProposalIterator
                        = swapProposalsForListing.listIterator(swapProposalsForListing.size());
                
                while (swapProposalIterator.hasPrevious()) {
                    var swapProposal = swapProposalIterator.previous();
                    
                    var proposedUtility = timetablePreferences.getTimeslotUtility(swapProposal.getProposedSlot().getTimeslotId());
                    var utilityChange = proposedUtility - currentTutorialSlotUtility;
                    
                    if (utilityChange >= bestUtilityChange) {
                        
                        bestSwapProposal = swapProposal;
                        bestUtilityChange = utilityChange;
                    }
                    
                }
                
                //checks if the swap proposal meets the minimum utility gain defined but overrides the minimum if own tutorial slot is a CANNOT
                //checks if the slot has not been unlocked/made unavailable in meantime
                if (bestSwapProposal == null && bestUtilityChange >= minimumSwapUtilityGain && currentTutorialSlotUtility > 0 || assignedTutorialSlots.get(listingId)) {
                    System.out.println(student.getMatriculationNumber() + " no good swap proposals found");
                }
                else {
                    var proposalReply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    proposalReply.setConversationId("timeslot-swap");
                    proposalReply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                    System.out.println(student.getMatriculationNumber() + "accepted proposal to swap slot for " + listingId);
                    
                    var proposeSwapToStudent = new ProposeSwapToStudent(bestSwapProposal, aid);
                    try {
                        getContentManager().fillContent(proposalReply, proposeSwapToStudent);
                        proposalReply.addReceiver(timetablerAgent);
                        send(proposalReply);
                        messagesSent++;
                        //moves advertised tutorial into waiting room
                        unconfirmedAcceptedSwapProposalsBySelf.put(listingId, bestSwapProposal);
                        ownCFPedTutorialSlots.remove(listingId);
                        //should wait for confirmation to do any swaps
                        swapProposalsForListing.remove(bestSwapProposal);
                        //todo double check this removes as expected
                    }
                    
                    catch (Codec.CodecException e) {
                        throw new RuntimeException(e);
                    }
                    catch (OntologyException e) {
                        throw new RuntimeException(e);
                    }
                }
                addBehaviour(new RejectSwapProposals(swapProposalsForListing));
                
            }
        }
    }
    
    private class RejectSwapProposals extends ParallelBehaviour
    {
        private ArrayList<SwapProposal> swapProposalsForListing;
        
        public RejectSwapProposals(ArrayList<SwapProposal> swapProposalsForListing) {
            super((ParallelBehaviour.WHEN_ALL));
            this.swapProposalsForListing = swapProposalsForListing;
        }
        
        @Override
        public void onStart() {
            swapProposalsForListing.forEach((swapProposal) -> {
                addSubBehaviour(new SendSwapProposalRejection(swapProposal));
            });
        }
    }
    
    private class SendSwapProposalRejection extends OneShotBehaviour
    {
        
        private SwapProposal swapProposal;
        
        public SendSwapProposalRejection(SwapProposal swapProposal) {
            
            this.swapProposal = swapProposal;
        }
        
        @Override
        public void action() {
            var proposalReply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            proposalReply.setConversationId("timeslot-swap");
            proposalReply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            proposalReply.addReceiver(timetablerAgent);
            
            var proposeSwapToStudent = new ProposeSwapToStudent(swapProposal, aid);
            try {
                getContentManager().fillContent(proposalReply, proposeSwapToStudent);
                send(proposalReply);
                System.out.println(student.getMatriculationNumber() + "sent swap proposal rejection for " + swapProposal.getProposalId());
    
            }
            catch (Codec.CodecException e) {
                throw new RuntimeException(e);
            }
            catch (OntologyException e) {
                throw new RuntimeException(e);
            }
            
        }
    }
    
    private class AcceptedSwapProposalResultReceiver extends CyclicBehaviour
    {
        //todo i'd watch out here in case it manages to get it in before it's marked as confirmed somehow
        @Override
        public void action() {
            //receive response
            var confirmTemplate = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                                          MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET)), MessageTemplate.and(
                    MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("timeslot-swap")));
            
            var swapResultMsg = myAgent.receive(confirmTemplate);
            
            if (swapResultMsg != null) {
                System.out.println(student.getMatriculationNumber() + " received swap result");
                
                ContentElement contentElement;

//                System.out.println(swapResultMsg.getContent()); //print out the message content in SL
                
                // Let JADE convert from String to Java objects
                // Output will be a ContentElement
                try {
                    contentElement = getContentManager().extractContent(swapResultMsg);
                    if (contentElement instanceof IsSwapResult) {
                        var isSwapResult = (IsSwapResult) contentElement;
                        var swapProposal = isSwapResult.getSwapProposal();
                        
                        var proposedSlot = swapProposal.getProposedSlot();
                        
                        var unwantedSlotListingId = swapProposal.getUnwantedListingId();
                        var unwantedSlot = unwantedTutorialsOnOffer.get(unwantedSlotListingId);
                        
                        if (swapResultMsg.getConversationId().equals("timeslot-swap")) {
                            if (swapResultMsg.getPerformative() == ACLMessage.INFORM && isSwapResult.isAccepted()) {
                                System.out.println(student.getMatriculationNumber() + " accepted swap is confirmed for " +unwantedSlotListingId);
                                
                                var oldUtility = timetablePreferences.getTotalUtility(student.getTutorialSlots(), timetablePreferences);
                                
                                //removes unwanted tutorial and adds new tutorial
                                assignedTutorialSlots.remove(unwantedSlot);
                                assignedTutorialSlots.put(proposedSlot, false);
                                
                                unconfirmedAcceptedSwapProposalsBySelf.remove(swapProposal.getUnwantedListingId());
                                
                                unwantedListingSwapProposals.remove(unwantedSlotListingId);
                                
                                //again not really used but ho hey
                                student.removeTutorialSlot(unwantedSlot);
                                student.addTutorialSlot(proposedSlot);
                                
                                timetableUtility = timetablePreferences.getTotalUtility(student.getTutorialSlots(), timetablePreferences);
                                lastSwapTime = System.currentTimeMillis();
                                
                                System.out.println(student.getMatriculationNumber() + "'s utility has changed by: " + (timetableUtility - oldUtility));
                                
                            }
                        }
                        else {
                            ownCFPedTutorialSlots.put(unwantedSlot, unwantedSlotListingId);
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

/////REQUEST/UNWANTED/CFP////
    
    ///////UTILITY///////
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
            System.out.println(aid + " registering with UtilityAgent");
            
            //receive response
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("register-utility"));
            
            var reply = blockingReceive(mt);
            
            if (reply != null && reply.getConversationId().equals("register-utility")) {
                System.out.println(aid + " registered with UtilityAgent");
                
            }
            else {
                block();
            }
        }
    }
    
    //listens for poll and sends utility & messagesSent
    private class MetricsPollListener extends CyclicBehaviour
    {
        @Override
        public void action() {
            
            var mt = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchSender(utilityAgent),
                                                             MessageTemplate.MatchPerformative(ACLMessage.REQUEST)), MessageTemplate.MatchConversationId("current-metrics"));
            
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender().equals(utilityAgent)) {
                System.out.println(student.getMatriculationNumber() + " received metrics poll request");
                
                addBehaviour(new MetricsSender());
                
            }
            else {
                block();
            }
        }
    }
    
    //listens for poll and sends utility & messagesSent
    private class MetricsSender extends OneShotBehaviour
    {
        @Override
        public void action() {
            
            var msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(utilityAgent);
            msg.setConversationId("current-metrics");
            msg.setOntology(ontology.getName());
            msg.setLanguage(codec.getName());
            
            var areCurrentFor = new AreCurrentFor();
            var studentMetrics = new StudentAgentMetrics();
            studentMetrics.setCurrentTimetableUtility((long) timetableUtility);
            studentMetrics.setMessagesSent((long) messagesSent);
            studentMetrics.setFinalMetrics(finalMetrics);
            studentMetrics.setTimetableIsValid(timetablePreferences.isValid(assignedTutorialSlots, timetablePreferences));
            studentMetrics.setInitialTimetableUtility(initialTimetableUtility);
            studentMetrics.setMaximumTimetableUtility(maximumTimetableUtility);
            studentMetrics.setPercentageOfOptimum();
            
            areCurrentFor.setStudent(getAID());
            areCurrentFor.setStudentMetrics(studentMetrics);
            
            try {
                getContentManager().fillContent(msg, areCurrentFor);
                System.out.println(aid + " sending metrics");
                
                send(msg);
            }
            catch (Codec.CodecException e) {
                e.printStackTrace();
            }
            catch (OntologyException e) {
                e.printStackTrace();
            }
            
            if (initialMetrics) {
                initialMetrics = false;
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
                System.out.println(student.getMatriculationNumber() + " received end notification");
                
                end = true;
                //sends final Metrics to utility agent
                finalMetrics = true;
                addBehaviour(new MetricsSender());
                
                System.out.println("Student " + student.getMatriculationNumber() + " total utility achieved:" + timetableUtility + " with: " + messagesSent + " messages sent.");
                
                var takedownConfirmMsg = new ACLMessage(ACLMessage.INFORM);
                takedownConfirmMsg.addReceiver(utilityAgent);
                
                takedownConfirmMsg.setConversationId("ending");
                
                takedownConfirmMsg.setContent("Shutting down " + getAID());
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
    
    //adjusts strategy taking into account current utility and slowness//stuckness
    public void AdjustStrategy() {
        //set utility threshold low for very low utility gains, seems counterintuitive but it gives more dynamism possibly to get out of the hole
        
        if (timetableUtility >= highUtilityThreshold) {
            //geared at making small gains to already a good threshold
            unwantedSlotUtilityThreshold = mediumUnwantedSlotUtilityThreshold;
            minimumSwapUtilityGain = lowMinimumSwapUtilityGain;
        }
        else {
            if (timetableUtility >= mediumUtilityThreshold && timetableUtility < highUtilityThreshold) {
                unwantedSlotUtilityThreshold = lowUnwantedSlotUtilityThreshold;
                minimumSwapUtilityGain = mediumMinimumSwapUtilityGain;
                
            }
            if (timetableUtility < mediumUtilityThreshold) {
                unwantedSlotUtilityThreshold = lowUnwantedSlotUtilityThreshold;
                minimumSwapUtilityGain = lowMinimumSwapUtilityGain;
            }
            //attempts to bump agent and system out of a mediocre or bad local optimum
            if ((System.currentTimeMillis() - lastSwapTime) > noSwapTimeThreshold) {
                if (unwantedSlotUtilityThreshold > lowMinimumSwapUtilityGain) {
                    System.out.println("Agent " + aid.getName() + " has reached the NO SWAP THRESHOLD and has lowered its minimum swap gain standards " + (System.currentTimeMillis() - lastSwapTime) / 1000 + " seconds since LAST SWAP");
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