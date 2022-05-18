package Agents;

import Models.Student;
import Objects.Preference;
import Objects.StudentTimetablePreferences;
import Ontology.Elements.*;
import Ontology.Elements.StudentStatistics;
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
    
    protected void setup()
    {
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
                        System.out.println(aid.getName() + " received tutorials ");
                        
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
                return;
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
            //RESPOND TO ADVERTISED (REQUESTED) SLOTS//
            addSubBehaviour(new UnwantedSlotListCFPReceiver());
            
            addSubBehaviour(new DelistedSlotListReceiver());
            
            addSubBehaviour(new SwapProposalResponseReceiver());
            
            addSubBehaviour(new AcceptedProposalResultReceiver());
            
            //ADVERTISE(REQUESTED) OWN SLOTS//
            addSubBehaviour(new UnwantedSlotsHandler());
            
            addSubBehaviour(new SwapProposalReceiver());
            
            addSubBehaviour(new UnwantedSwapResultReceiver());
            
        }
    }
    
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
                return;
            }
            if (msg.getSender().equals(timetablerAgent)) {
                
                try {
                    ContentElement contentElement;
                    
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof ProposeSwapToTimetabler) {
                        var proposeSwapToTimetabler = (ProposeSwapToTimetabler) contentElement;
                        
                        var unwantedTimeslotListing = proposeSwapToTimetabler.getUnwantedTimeslotListing();
                        
                        unwantedTutorialsOnOffer.put(unwantedTimeslotListing.getUnwantedListingId(), unwantedTimeslotListing.getTutorialSlot());
                        
                        myAgent.addBehaviour(new EvaluateCurrentSwapCFPs());
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
                return;
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
                
                try {
                    ContentElement contentElement = null;

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
                return;
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
            //filters slots on offer for tutorials of the same module as current tutorial slot
            var unwantedTutorialsOnOfferFiltered = unwantedTutorialsOnOffer.entrySet()
                                                                           .stream()
                                                                           .filter(element -> currentTutorialSlot.getModuleId().equals(element.getValue().getModuleId()))
                                                                           .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
            
            if (unwantedTutorialsOnOfferFiltered.size() <= 0) {
                return;
            }
            var bestSwapListing = new UnwantedTimeslotListing();
            //sets min. threshold for utility change
            var bestUtilityChange = minimumSwapUtilityGain;
            
            // Iterating HashMap of tutorialoffers through for loop
            for (Map.Entry<Long, TutorialSlot> set :
                    unwantedTutorialsOnOfferFiltered.entrySet()) {
                
                var tutorialSlot = set.getValue();
                var listingId = set.getKey();
                
                var proposedUtility = timetablePreferences.getTimeslotUtility(tutorialSlot.getTimeslotId());
                var utilityChange = proposedUtility - timetablePreferences.getTimeslotUtility(currentTutorialSlot.getTimeslotId());
                
                if (utilityChange >= bestUtilityChange) {
                    bestSwapListing = new UnwantedTimeslotListing(tutorialSlot, listingId);
                    bestUtilityChange = utilityChange;
                }
            }
            //CHECK IF LOCKED IF YES THEN SEND A REQUEST TO MAKE UNAVAILABLE AND UNLOCK
            //IN SWAP PROPOSAL RECEIVER CHECK IF THE SLOT IS UNLOCKED AND IF IT IS REJECT ALL OFFERs
            //could add an extra condition here to only do it if it's a Really good increase+save the second best utility change and propose that instead if it doesn't meet the threshold
            if (bestSwapListing == null) {
                return;
            }
            var proposeSwap = new ProposeSwapToTimetabler();
            proposeSwap.setProposedSlot(currentTutorialSlot);
            proposeSwap.setProposer(this.myAgent.getAID());
            proposeSwap.setUnwantedTimeslotListing(bestSwapListing);
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
            
            var unwantedSlotListingId = ownAdvertisedTutorials.get(currentTutorialSlot);
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
                    count++;
                }
                else {
                    if (reply.getPerformative() == ACLMessage.CONFIRM) {
                        agreed = true;
                        
                        addBehaviour(new SendProposal(proposeSwap));
                    }
                    else if (reply.getPerformative() == ACLMessage.REFUSE) {
                        if (reply.getContent() != null) {
                            System.out.println("Delist request refused because: " + reply.getContent()); //print out the message content in SL
                            
                        }
                        
                    }
                    count++;
                }
                //todo watch out for stuckness here
            }
        }
    }
    
    //evaluates current CFPs for proposal
    private class EvaluateCurrentSwapCFPs extends ParallelBehaviour
    {
        public EvaluateCurrentSwapCFPs() {
            super(ParallelBehaviour.WHEN_ALL);
        }
        
        @Override
        public void onStart() {
            var timetablePreferences = student.getStudentTimetablePreferences();
            
            //updates strategy if necessary
            AdjustStrategy();
            
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
    class SwapProposalRejectionHandler extends OneShotBehaviour
    {
        public SwapProposalRejectionHandler(ACLMessage response) {
        
        }
        
        @Override
        public void action() {
        
        }
    }
    
    private
    class SwapProposalInformHandler extends OneShotBehaviour
    {
        public SwapProposalInformHandler(ACLMessage response) {
        
        }
        
        @Override
        public void action() {
        
        }
    }
    
    private
    class SwapProposalAcceptanceHandler extends OneShotBehaviour
    {
        public SwapProposalAcceptanceHandler(ACLMessage response) {
        
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
                return;
            }
            try {
                
                var contentElement = getContentManager().extractContent(response);
                
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
                            addBehaviour(new AcceptedProposalResultReceiver());
                            
                            var requestedSlot = unwantedTutorialsOnOffer.get(requestedSlotListingId);
                                
                                System.out.println(student.getMatriculationNumber() + "'s proposal has been accepted: " + proposalId);

//                                if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && acceptedProposal) {

//                                    //removes proposed tutorial and adds new tutorial
//                                    if (proposedSlot.getModuleId() != requestedSlot.getModuleId()) {
//                                        throw new Exception("TUTORIAL SWAP MODULE MISMATCH");
//                                        //this is terrible handling but also really shouldn't happen there's multiple checks for it
//                                    }
                            
                            }
                            
                            else {
                                throw new Exception("PERFORMATIVE AND RESULT MISMATCH");
                            }
                        }
                    }
                    
                }
            catch(UngroundedException e){
                    e.printStackTrace();
                }
            catch(OntologyException e){
                    e.printStackTrace();
                }
            catch(Codec.CodecException e){
                    e.printStackTrace();
                }
            catch(Exception e){
                    e.printStackTrace();
                }
                
                if (response.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    addBehaviour(new SwapProposalRejectionHandler(response));
                }
                else if (response.getPerformative() == ACLMessage.INFORM) {
                    addBehaviour(new SwapProposalRejectionHandler(response));
                    
                }
                if (response.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    addBehaviour(new SwapProposalRejectionHandler(response));
                    
                }
                
                else {
                    
                    ContentElement contentElement = null;

//                System.out.println(reply.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    
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
                                if (swapResultMsg.getConversationId().equals("timeslot-swap") && assignedTutorialSlots.get(swapProposal.getProposedSlot())) {
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
                    return;
                }
            }
        }
        
        /////REQUEST/UNWANTED/CFP////
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
                
                if (proposal != null && proposal.getConversationId().equals("timeslot-swap")) {
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
                                        
                                        //deemed overkill
//                                    //receive confirmation of receipt, unsure if strictly necessary but it's already written so
//                                    var confirmTemplate = MessageTemplate.and(
//                                            MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
//                                            MessageTemplate.MatchSender(timetablerAgent));
//
//                                    var confirm = myAgent.receive(confirmTemplate);
//
//                                    if (confirm != null && confirm.getConversationId().equals("timeslot-swap")) {
//                                        System.out.println("timetabler confirmed " + student.getMatriculationNumber() + "'s proposal rejection for " + proposedTutorialSlot);
//
//                                    }
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
                    return;
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
                        MessageTemplate.MatchSender(timetablerAgent), MessageTemplate.MatchConversationId("timeslot-swap")));
                
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
                            
                            if (swapResultMsg.getConversationId().equals("timeslot-swap")) {
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
                    return;
                }
            }
        }
        
        //send unwanted slots to be advertised
        public class UnwantedSlotsHandler extends ParallelBehaviour
        {
            public UnwantedSlotsHandler() {
                super(ParallelBehaviour.WHEN_ALL);
            }
            
            //requests unwanted slots be listed for offers
//    private class ListUnwantedSlotRequester extends Behaviour
            
            @Override
            public void onStart() {
                //this is only a oneshot initially because it gets triggered when assigned tutorials or the strategy changes
                //scratch that we tick consistently
                addSubBehaviour(new UnwantedSlotFinder(this.myAgent, unwantedSlotCheckPeriod));
                addSubBehaviour(new ListUnwantedSlotRequestConfirmationReceiver());
                
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
                    messagesSent++;
                    
                    System.out.println(aid.getName() + " sent unwanted tutorial listing request for:" + unwantedSlot.getTimeslotId());
                    
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
        
        //
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

//            if (totalUtility < utilityThreshold) {
                //if own slots was likely to be a bigger collection would be worth parallelising but as it is there's no need
                assignedTutorialSlots.forEach((tutorialSlot, isLocked) -> {
                    if (timetablePreferences.getTimeslotUtility(tutorialSlot.getTimeslotId()) <= unwantedSlotUtilityThreshold && !isLocked) {
                        
                        addBehaviour(new ListUnwantedSlotRequestSender(tutorialSlot));
                        
                    }
                    
                });
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
                            
                            ownAdvertisedTutorials.put(unwantedTimeslot, unwantedTimeslotListing.getUnwantedListingId());
                            
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
        /////REQUEST/UNWANTED/CFP////
        
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
                    return;
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
                    return;
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
                    return;
                }
                
            }
            
        }
        
        //updates strategy according to current state
        //todo could also have the utility overlord meddle here and command/request the agent to laxen the strategy if the global utility isn't looking good or is rising too slowly; could actually target local maxima to make them less selfish but that is OUTWITH THIS PROJECT AND MY TIME AND ABILITIES
        
        //adjusts strategy taking into account current utility and slowness//stuckness
        public void AdjustStrategy() {
            //set utility threshold low for very low utility gains, seems counterintuitive but it gives more dynamism possibly to get out of the hole
            if (totalUtility < mediumUtilityThreshold) {
                unwantedSlotUtilityThreshold = lowUnwantedSlotUtilityThreshold;
                minimumSwapUtilityGain = lowMinimumSwapUtilityGain;
            }
            if (totalUtility >= mediumUtilityThreshold && totalUtility < highUtilityThreshold) {
                unwantedSlotUtilityThreshold = mediumUnwantedSlotUtilityThreshold;
                minimumSwapUtilityGain = mediumMinimumSwapUtilityGain;
            }
            if (totalUtility >= highUtilityThreshold) {
                unwantedSlotUtilityThreshold = mediumUnwantedSlotUtilityThreshold;
                minimumSwapUtilityGain = lowMinimumSwapUtilityGain;
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