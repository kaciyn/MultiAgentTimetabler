package Agents;

import Models.Module;
import Models.Student;
import Ontology.Elements.*;
import Ontology.Elements.SwapProposal;
import Ontology.Elements.TutorialSlot;
import Ontology.Elements.UnwantedTimeslotListing;
import Ontology.MultiAgentTimetablerOntology;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class TimetablerAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    
    private HashMap<AID, Student> studentModelAgents;
    private HashMap<AID, Long> studentAgents;
    //don't bother updating these, just for initial ref
    private ArrayList<Student> students;
    private ArrayList<Module> modules;
    
    /////THESE NEED TO BE UPDATED EACH TIME
    //TutorialSlot, Student ids
    private HashMap<TutorialSlot, ArrayList<Long>> tutorialSlotStudentIdsMap;
    
    //listingId,tutorialSlot
    private HashMap<Long, TutorialSlot> unwantedTutorialSlots;
    
    //listingId,student AID
    private HashMap<Long, AID> unwantedTutorialOwners;
    private LinkedList<ACLMessage> unhandledListSlotRequests;
    
    //listingId,ProposedSwapOffers
    private HashMap<Long, ArrayList<SwapProposal>> tutorialSlotSwapProposals;
    
    //proposalId,studentAID
    private HashMap<Long, AID> swapProposers;
    
    private long timeSwapBehaviourEnded;
    private long timeSwapBehaviourStarted;
    
    private AID utilityAgent;
    private boolean end;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        
        end = false;
        tutorialSlotStudentIdsMap = new HashMap<>();
        studentAgents = new HashMap<>();
        
        unwantedTutorialSlots = new HashMap<>();
        unwantedTutorialOwners = new HashMap<>();
        tutorialSlotSwapProposals = new HashMap<>();
        swapProposers = new HashMap<>();
        unhandledListSlotRequests = new LinkedList<>();
//        unwantedBroadcastQueue = new LinkedList<>();
        
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
        modules = new ArrayList<>();
        if (args != null && args.length > 0) {
            modules = (ArrayList<Module>) args[0];
            
            System.out.println("Modules loaded");
        }
        if (args != null && args.length > 1) {
            students = (ArrayList<Student>) args[1];
            
            System.out.println("Students loaded");
        }
        
        else {
// Make the agent terminate immediately
            System.out.println("No modules or students found, terminating.");
            doDelete();
        }
        
        //create tutorialSlotStudentIdsMap
        modules.forEach(module -> {
            module.getTutorials().forEach(tutorial -> {
                var tutorialSlot = new TutorialSlot(tutorial.getModuleId(), tutorial.getTimeslotId());
                tutorialSlot.setTimeslotId(tutorial.getTimeslotId());
                
                tutorialSlotStudentIdsMap.put(tutorialSlot, tutorial.getStudentIds());
                
            });
        });
     
        
        addBehaviour(new TickerBehaviour(this, 10000)
        {
            @Override
            protected void onTick()
            {  // Search for utility agent
                var utilTemplate = new DFAgentDescription();
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
                addBehaviour(new UtilityRegistrationServer());
                addBehaviour(new EndListener());
                
            }
        });
        System.out.println("Waiting for student agents' registration...");
        addBehaviour(new StudentRegistrationReceiver());
        
        addBehaviour(new UtilityRegistrationServer());
        
        addBehaviour(new SwapServerBehaviour());
        
    }
    
    //registers student agents and sends them their initial tutorial assignments, links student with aid
    private class StudentRegistrationReceiver extends CyclicBehaviour
    {
        public void action()
        {
            System.out.println("Timetabler receiving registrations ");
    
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("register"));
            var msg = myAgent.receive(mt);
            
//            if (msg == null) {
//                block();
//            }
//            else {
            if (msg != null && msg.getConversationId().equals("register")) {
        
                System.out.println("Timetabler received registration message ");
                
                AID newStudentAID = msg.getSender();
                var newStudentId = Long.parseLong(msg.getContent());
                var newStudent = students.stream().filter(student -> newStudentId == student.getMatriculationNumber()).findFirst().orElse(null);
                
                var reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.addReplyTo(newStudentAID);
                reply.setLanguage(codec.getName());
                reply.setOntology(ontology.getName());
                
                if (newStudent == null) {
                    
                    reply.setContent("Student is not enrolled");
                    
                    System.out.println(newStudentAID.getName() + " not registered ");
                    
                    send(reply);
                    block();
                }
                
                if (studentAgents.containsKey(newStudentAID)) {
                    System.out.println(newStudentAID.getName() + " already registered ");
                    
                    send(reply);
                    block();
                }
                
                studentAgents.put(newStudentAID, newStudent.getMatriculationNumber());
                
                List<TutorialSlot> assignedTutorialSlots = tutorialSlotStudentIdsMap.keySet().stream()
                                                                                    .filter(tutorialSlot -> tutorialSlotStudentIdsMap.get(tutorialSlot)
                                                                                                                                     .contains(newStudentId))
                                                                                    .collect(Collectors.toList());
                
                var isAssignedTo = new IsAssignedTo(newStudentAID, (ArrayList<TutorialSlot>) assignedTutorialSlots);
                
                try {
                    // Let JADE convert from Java objects to string
                    getContentManager().fillContent(reply, isAssignedTo);
                    send(reply);
//                    System.out.println(newStudentAID.getName() + " registered ");
                    
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
    
    public class SwapServerBehaviour extends ParallelBehaviour
    {
        public SwapServerBehaviour() {
            super(ParallelBehaviour.WHEN_ALL);
        }
        
        @Override
        public void onStart() {
            System.out.println("Accepting swap requests...");
//            addSubBehaviour(new UnwantedSlotRequestHandler());
            
            addSubBehaviour(new ListUnwantedSlotRequestHandlerMaker());
            addSubBehaviour(new DelistSlotRequestReceiver());
            addSubBehaviour(new SwapOfferReceiver());
            addSubBehaviour(new SwapProposalResponseReceiver());
            
            addSubBehaviour(new ListUnwantedSlotRequestReceiver());
            timeSwapBehaviourStarted = System.currentTimeMillis();
            
        }
    }
    
    public class ListUnwantedSlotRequestHandlerMaker extends ParallelBehaviour
    {
        public ListUnwantedSlotRequestHandlerMaker() {
            super(ParallelBehaviour.WHEN_ALL);
        }
        
        @Override
        public void onStart() {
            while (!end) {
                if (unhandledListSlotRequests.size() > 0) {
                    //continuously pops off received unhandled requests until shutdown ordered
                    addSubBehaviour(new ListUnwantedSlotRequestHandler(unhandledListSlotRequests.pop()));
                    //is not just a while >0 because  there's always new ones coming potentially
                }
                
            }
        }
    }
    
    public class ListUnwantedSlotRequestHandler extends OneShotBehaviour
    {
        
        private final ACLMessage msg;
        
        public ListUnwantedSlotRequestHandler(ACLMessage msg) {
            this.msg = msg;
        }
        
        @Override
        public void action() {
            
            var studentAID = msg.getSender();
            
            var reply = msg.createReply();
            reply.setLanguage(codec.getName());
            reply.setOntology(ontology.getName());
            reply.setConversationId("timeslot-swap");
            reply.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            reply.addReceiver(studentAID);
            
            ContentElement contentElement;
            try {
                contentElement = getContentManager().extractContent(msg);
                
                if (!(contentElement instanceof Action)) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Invalid request");
                }
                else {
                    var action = ((Action) contentElement).getAction();
                    
                    if (!(action instanceof ListUnwantedSlot)) {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("Invalid action");
                    }
                    else {
                        var listAdvertisedSlot = (ListUnwantedSlot) action;
                        
                        //checks the agent isn't trying to advertise someone else's slot
                        if (!listAdvertisedSlot.getRequestingStudentAgent().equals(msg.getSender())) {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("Action Requester and sender mismatch");
                        }
                        else {
                            
                            //creates an id to reference the unwanted slot offer so the offering student's identity is not revealed
                            var unwantedListingId = Long.valueOf(listAdvertisedSlot.getUnwantedTutorialSlot().getTimeslotId() + ThreadLocalRandom.current().nextInt());
                            var unwantedSlot = listAdvertisedSlot.getUnwantedTutorialSlot();
                            
                            ////respond to requester
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                            reply.setLanguage(codec.getName());
                            reply.setOntology(ontology.getName());
                            reply.addReceiver(listAdvertisedSlot.getRequestingStudentAgent());
                            
                            System.out.println("Timetabler is imminently listing unwanted slot for:" + unwantedSlot.getTimeslotId() + " from agent " + listAdvertisedSlot.getRequestingStudentAgent());
                            
                            var unwantedTimeslotListing = new UnwantedTimeslotListing();
                            unwantedTimeslotListing.setUnwantedListingId(unwantedListingId);
                            unwantedTimeslotListing.setTutorialSlot(unwantedSlot);
                            
                            //lists in unwanted slots
                            unwantedTutorialSlots.put(unwantedListingId, unwantedSlot);
                            //creates proposal list for slot
                            tutorialSlotSwapProposals.put(unwantedListingId, new ArrayList<>());
                            //note listing owner
                            unwantedTutorialOwners.put(unwantedListingId, listAdvertisedSlot.getRequestingStudentAgent());
                            
                            var isListedFor = new IsListedFor();
                            
                            isListedFor.setStudent(listAdvertisedSlot.getRequestingStudentAgent());
                            isListedFor.setUnwantedTimeslotListing(unwantedTimeslotListing);
                            
                            getContentManager().fillContent(reply, isListedFor);
                            
                            send(reply);
                            
                            addBehaviour(new UnwantedSlotCFPBroadcaster(unwantedTimeslotListing));
                            
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
    
    private class UnwantedSlotCFPBroadcaster extends ParallelBehaviour
    {
        
        public UnwantedSlotCFPBroadcaster(UnwantedTimeslotListing unwantedTimeslotListing) {
            var proposeSwapToTimetabler = new ProposeSwapToTimetabler(unwantedTimeslotListing);
            
            var broadcast = new ACLMessage(ACLMessage.CFP);
            broadcast.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            broadcast.setLanguage(codec.getName());
            broadcast.setOntology(ontology.getName());
            broadcast.setConversationId("timeslot-swap");
            
            studentAgents.forEach((studentAgent, student) -> {
                addSubBehaviour(new UnwantedSlotCFPer(proposeSwapToTimetabler, broadcast, studentAgent));
            });
            
        }
    }
    
    private class UnwantedSlotCFPer extends OneShotBehaviour
    {
        
        private ProposeSwapToTimetabler proposeSwapToTimetabler;
        private ACLMessage broadcast;
        private AID studentAgent;
        
        public UnwantedSlotCFPer(ProposeSwapToTimetabler proposeSwapToTimetabler, ACLMessage broadcast, AID studentAgent) {
            this.proposeSwapToTimetabler = proposeSwapToTimetabler;
            this.broadcast = broadcast;
            this.studentAgent = studentAgent;
        }
        
        @Override
        public void action() {
            var callForAction = new Action();
            callForAction.setAction(proposeSwapToTimetabler);
            callForAction.setActor(studentAgent); // the agent that you request to perform the action
            
            try {
                // Let JADE convert from Java objects to string
                getContentManager().fillContent(broadcast, callForAction);
                broadcast.addReceiver(studentAgent);
                send(broadcast);
                System.out.println("Broadcasting unwanted tutorial CFP for " + proposeSwapToTimetabler.getUnwantedTimeslotListing().getUnwantedListingId() + " to Student " + studentAgent.getName());
            }
            catch (Codec.CodecException ce) {
                ce.printStackTrace();
            }
            catch (OntologyException oe) {
                oe.printStackTrace();
            }
            
        }
    }
    
    private class ListUnwantedSlotRequestReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST)), MessageTemplate.MatchConversationId("timeslot-swap"));
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg == null) {
                block();
                
            }
            else {
                unhandledListSlotRequests.addLast(msg);
                
            }
            
        }
        
    }
    
    private class DelistSlotRequestHandler extends OneShotBehaviour
    {
        
        private ACLMessage msg;
        
        public DelistSlotRequestHandler(ACLMessage msg) {
            
            this.msg = msg;
        }
        
        @Override
        public void action() {
            
            var studentAID = msg.getSender();
            
            var reply = msg.createReply();
            reply.setLanguage(codec.getName());
            reply.setOntology(ontology.getName());
            reply.setConversationId("delist-advertised-slot");
            reply.addReceiver(studentAID);
            
            ContentElement contentElement;
            // Let JADE convert from String to Java objects
            // Output will be a ContentElement
            try {
                contentElement = getContentManager().extractContent(msg);
                
                if (!(contentElement instanceof Action)) {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Invalid request");
                }
                else {
                    var action = ((Action) contentElement).getAction();
                    
                    if (!(action instanceof DelistUnwantedSlot)) {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("Invalid action");
                        return;
                    }
                    var delistUnwantedSlot = (DelistUnwantedSlot) action;
                    
                    var slotListingToDelist = delistUnwantedSlot.getSlotToDelist();
                    
                    ////respond to requester
                    reply.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    reply.setLanguage(codec.getName());
                    reply.setOntology(ontology.getName());
                    
                    //checks the agent isn't trying to delist someone else's slot
                    if (unwantedTutorialOwners.get(slotListingToDelist.getUnwantedListingId()).equals(msg.getSender())) {
                        unwantedTutorialSlots.remove(slotListingToDelist.getUnwantedListingId());
                        unwantedTutorialOwners.remove(slotListingToDelist.getUnwantedListingId());
                        
                        reply.setPerformative(ACLMessage.CONFIRM);
                        
                        System.out.println("Timetabler sending delist confirmation");
                        
                        addBehaviour(new DelistedOfferBroadcaster(slotListingToDelist));
                        
                    }
                    else {
                        reply.setPerformative(ACLMessage.REFUSE);
                        
                    }
                    send(reply);
                    System.out.println("Timetabler sending delist refusal");
                    
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
    
    private class DelistSlotRequestReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchConversationId("delist-advertised-slot"),
                                                                         MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST)),
                                                     MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                addBehaviour(new DelistSlotRequestHandler(msg));
            }
            else {
                block();
            }
        }
    }
    
    private class DelistedOfferBroadcaster extends ParallelBehaviour
    {
        
        private UnwantedTimeslotListing slotListingToDelist;
        
        public DelistedOfferBroadcaster(UnwantedTimeslotListing slotListingToDelist) {
            super(ParallelBehaviour.WHEN_ALL);
            this.slotListingToDelist = slotListingToDelist;
        }
        
        @Override
        public void onStart() {
            var broadcast = new ACLMessage(ACLMessage.INFORM);
            broadcast.setLanguage(codec.getName());
            broadcast.setOntology(ontology.getName());
            broadcast.setConversationId("delist-advertised-slot");
            
            var isDelisted = new IsDelisted();
            isDelisted.setUnwantedTimeslotListing(slotListingToDelist);
            
            try {
                // Let JADE convert from Java objects to string
                getContentManager().fillContent(broadcast, isDelisted);
                
                studentAgents.forEach((studentAgent, student) -> {
                    addSubBehaviour(new DelistedOfferSender(studentAgent, broadcast));
                    
                });
                System.out.println("Timetabler broadcasted delisted slot " + isDelisted.getUnwantedTimeslotListing().getUnwantedListingId());
                
            }
            catch (Codec.CodecException ce) {
                ce.printStackTrace();
            }
            catch (OntologyException oe) {
                oe.printStackTrace();
            }
            
        }
    }
    
    private class DelistedOfferSender extends OneShotBehaviour
    {
        private AID studentAgent;
        private ACLMessage broadcast;
        
        public DelistedOfferSender(AID studentAgent, ACLMessage broadcast) {
            
            this.studentAgent = studentAgent;
            this.broadcast = broadcast;
        }
        
        @Override
        public void action() {
            broadcast.addReceiver(studentAgent);
            
            send(broadcast);

//            System.out.println("Broadcast delisted tutorial slot " + isNoLongerOnOffer.getUnwantedTimeslotListing().getUnwantedListingId()
////                                                   + " to Student " + studentAgent.getName()
//            );
        }
    }
    
    private class SwapOfferReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)), MessageTemplate.MatchConversationId("timeslot-swap"));
            
            var proposalMsg = myAgent.receive(mt);
            if (proposalMsg == null) {
                block();
            }
            else {
                addBehaviour(new SwapOfferHandler(proposalMsg));
                
            }
        }
    }
    
    private class SwapOfferHandler extends OneShotBehaviour
    {
        private ACLMessage proposalMsg;
        
        public SwapOfferHandler(ACLMessage proposalMsg) {
            
            this.proposalMsg = proposalMsg;
        }
        
        @Override
        public void action() {

//                System.out.println(proposalMsg.getContent()); //print out the message content in SL
            
            try {
                var contentElement = getContentManager().extractContent(proposalMsg);
                
                if (!(contentElement instanceof Action)) {
                    return;
                }
                var action = ((Action) contentElement).getAction();
                
                if (!(action instanceof ProposeSwapToTimetabler)) {
                    return;
                }
                var proposeSwapToTimetabler = (ProposeSwapToTimetabler) action;
                
                addBehaviour(new SwapProposalToTimetableHandler(proposeSwapToTimetabler));
                
            }
            catch (UngroundedException ungroundedException) {
                ungroundedException.printStackTrace();
            }
            catch (OntologyException ontologyException) {
                ontologyException.printStackTrace();
            }
            catch (Codec.CodecException codecException) {
                codecException.printStackTrace();
            }
            
        }
        
    }
    
    private class SwapProposalToTimetableHandler extends OneShotBehaviour
    {
        
        private ProposeSwapToTimetabler proposeSwapToTimetabler;
        private ACLMessage proposalMsg;
        
        public SwapProposalToTimetableHandler(ProposeSwapToTimetabler proposeSwapToTimetabler, ACLMessage proposalMsg) {
            
            this.proposeSwapToTimetabler = proposeSwapToTimetabler;
            this.proposalMsg = proposalMsg;
        }
        
        public SwapProposalToTimetableHandler(ProposeSwapToTimetabler proposeSwapToTimetabler) {
        
        }
        
        @Override
        public void action() {
            var proposalReply = proposalMsg.createReply();
            proposalReply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            
            var unwantedListing = proposeSwapToTimetabler.getUnwantedTimeslotListing();
            var proposedTutorialSlot = proposeSwapToTimetabler.getProposedSlot();
            var proposingStudent = proposalMsg.getSender();
            
            //check if the module matches, will also catch if it's been removed
            if (proposedTutorialSlot.getModuleId() != unwantedTutorialSlots.get(unwantedListing.getUnwantedListingId()).getModuleId()) {
                proposalReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                proposalReply.setContent("REJECTED SWAP PROPOSAL: MODULE MISMATCH");
                
            }
            else {
                if (proposeSwapToTimetabler.getProposer().equals(proposalMsg.getSender())) {
                    addBehaviour(new CreateSwapProposal(proposedTutorialSlot, unwantedListing, proposingStudent));
                    
                }
            }
        }
    }
    
    private class CreateSwapProposal extends OneShotBehaviour
    {
        private TutorialSlot proposedTutorialSlot;
        private UnwantedTimeslotListing unwantedListing;
        private AID proposingStudent;
        
        public CreateSwapProposal(TutorialSlot proposedTutorialSlot, UnwantedTimeslotListing unwantedListing, AID proposingStudent) {
            this.proposedTutorialSlot = proposedTutorialSlot;
            this.unwantedListing = unwantedListing;
            this.proposingStudent = proposingStudent;
        }
        
        @Override
        public void action() {
            //creates an id to reference the proposal offer so the proposing student's identity is not revealed
            var proposalId = Long.valueOf(proposedTutorialSlot.getTimeslotId() + ThreadLocalRandom.current().nextInt());
            
            var swapProposal = new SwapProposal(proposalId, unwantedListing.getUnwantedListingId(), proposedTutorialSlot);
            //add to proposers list
            swapProposers.put(proposalId, proposingStudent);
            
            var unwantedTutorialOwner = unwantedTutorialOwners.get(unwantedListing.getUnwantedListingId());
            
            //adds to proposal list
            var proposalsEntry = tutorialSlotSwapProposals.get(proposalId);
            ArrayList<SwapProposal> proposals;
            if (proposalsEntry == null) {
                proposals = new ArrayList<>();
            }
            else {
                proposals = tutorialSlotSwapProposals.get(proposalId);
            }
            proposals.add(swapProposal);
            tutorialSlotSwapProposals.put(proposalId, proposals);
            
            //INFORM proposer of proposal
            var isProposed = new IsProposed();
            isProposed.setSwapProposal(swapProposal);
            isProposed.setProposer(proposingStudent);
            
            var proposalReply = new ACLMessage();
            proposalReply.setPerformative(ACLMessage.INFORM);
            proposalReply.setLanguage(codec.getName());
            proposalReply.setOntology(ontology.getName());
            
            try {
                getContentManager().fillContent(proposalReply, isProposed);
            }
            catch (Codec.CodecException e) {
                e.printStackTrace();
            }
            catch (OntologyException e) {
                e.printStackTrace();
            }
            
            send(proposalReply);
            System.out.println("Timetabler sending proposal reply");
            
            //PROPOSE to requester
            var unwantedTutorialSlot = unwantedTutorialSlots.get(swapProposal.getUnwantedListingId());
            
            var requestingStudentAgent = unwantedTutorialOwners.get(swapProposal.getUnwantedListingId());
            
            var swapProposalWithId = swapProposal;
            swapProposalWithId.setProposalId(proposalId);
            
            var proposeSwapToStudent = new ProposeSwapToStudent(swapProposalWithId, requestingStudentAgent);
            
            var proposalOfferMsg = new ACLMessage(ACLMessage.PROPOSE);
            proposalOfferMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            proposalOfferMsg.addReceiver(requestingStudentAgent);
            proposalOfferMsg.setLanguage(codec.getName());
            proposalOfferMsg.setOntology(ontology.getName());
            proposalOfferMsg.setConversationId("timeslot-swap");
            
            var proposal = new Action();
            proposal.setAction(proposeSwapToStudent);
            proposal.setActor(requestingStudentAgent);
            
            try {
                getContentManager().fillContent(proposalOfferMsg, proposal);
            }
            catch (Codec.CodecException e) {
                e.printStackTrace();
            }
            catch (OntologyException e) {
                e.printStackTrace();
            }
            
            send(proposalOfferMsg);
            System.out.println("Timetabler forwarding swap proposal");
            
        }
    }
    
    private class SwapProposalResponseReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            MessageTemplate proposalResponseMt = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                                                                                         MessageTemplate.MatchConversationId("timeslot-swap")), MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL), MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)));
            
            var proposalResultResponse = myAgent.receive(proposalResponseMt);
            
            if (proposalResultResponse != null) {
                try {
                    ContentElement contentElement = null;

//                    System.out.println(proposalResultResponse.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(proposalResultResponse);
                    
                    if (contentElement instanceof IsSwapResult) {
                        var isSwapResult = (IsSwapResult) contentElement;
                        var swapProposal = isSwapResult.getSwapProposal();
                        //send confirm to the successfully swapped students, notifies all others that tutorial is not on offer anymore
                        if (proposalResultResponse.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && isSwapResult.isAccepted()) {
                            var unwantedListingId = swapProposal.getUnwantedListingId();
                            
                            var requestingStudentAID = unwantedTutorialOwners.get(swapProposal.getUnwantedListingId());
                            var proposingStudentAID = swapProposers.get(swapProposal.getProposalId());
                            
                            //soft-swap and check for inconsistency
                            var unwantedSlot = unwantedTutorialSlots.get(unwantedListingId);
                            var unwantedSlotStudentIds = tutorialSlotStudentIdsMap.get(unwantedSlot);
                            unwantedSlotStudentIds.remove(studentAgents.get(requestingStudentAID));
                            unwantedSlotStudentIds.add(studentAgents.get(proposingStudentAID));
                            
                            var proposedSlotStudentIds = tutorialSlotStudentIdsMap.get(swapProposal.getProposedSlot());
                            proposedSlotStudentIds.remove(studentAgents.get(proposingStudentAID));
                            proposedSlotStudentIds.add(studentAgents.get(requestingStudentAID));
                            
                            assert SoftSwapCheckPassed(unwantedSlotStudentIds, unwantedSlot, proposedSlotStudentIds, swapProposal.getProposedSlot());
                            
                            if (SoftSwapCheckPassed(unwantedSlotStudentIds, unwantedSlot, proposedSlotStudentIds, swapProposal.getProposedSlot()) && CheckHardConstraintsPassed()) {
                                
                                //accept proposal of proposingStudent
                                var unwantedTimeslotListing = new UnwantedTimeslotListing(unwantedSlot, unwantedListingId);
                                var proposeSwapToTimetabler = new ProposeSwapToTimetabler(unwantedTimeslotListing,
                                                                                          swapProposal.getProposedSlot(), proposingStudentAID);
                                proposeSwapToTimetabler.setProposedSlot(swapProposal.getProposedSlot());
                                proposeSwapToTimetabler.setProposer(proposingStudentAID);
                                
                                var acceptProposalForward = new ACLMessage((ACLMessage.ACCEPT_PROPOSAL));
                                acceptProposalForward.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                acceptProposalForward.addReceiver(proposingStudentAID);
                                acceptProposalForward.setConversationId("timeslot-swap");
                                acceptProposalForward.setLanguage(codec.getName());
                                acceptProposalForward.setOntology(ontology.getName());
                                //shouldn't cause an issue since it's directly replying to the accept message
                                
                                //todo this is weird i don't think it should be the swap result but whatever the proposer sent instead
                                getContentManager().fillContent(acceptProposalForward, isSwapResult);
                                
                                send(acceptProposalForward);
                                System.out.println("Timetabler sending proposal acceptance");
                                
                                //swap result inform to both students
                                var swapConfirmation = proposalResultResponse.createReply();
                                swapConfirmation.setPerformative(ACLMessage.INFORM);
                                swapConfirmation.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                swapConfirmation.setConversationId("timeslot-swap");
                                acceptProposalForward.setLanguage(codec.getName());
                                acceptProposalForward.setOntology(ontology.getName());
                                
                                swapConfirmation.addReceiver(proposingStudentAID);
                                swapConfirmation.addReceiver(requestingStudentAID);
                                
                                isSwapResult.setAccepted(true);
                                getContentManager().fillContent(swapConfirmation, isSwapResult);
                                send(swapConfirmation);
                                
                                System.out.println("Swapped " + swapProposal.getProposalId());
                                
                                ////INTERAL SWAP UPDATES////
                                //Broadcast first then internal reference updates
                                var rejectionNewsletterBase = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                rejectionNewsletterBase.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                rejectionNewsletterBase.setLanguage(codec.getName());
                                rejectionNewsletterBase.setOntology(ontology.getName());
                                rejectionNewsletterBase.setConversationId("timeslot-swap");
                                
                                //rejects other offers
                                //i don't particularly like this implementation
                                tutorialSlotSwapProposals.get(unwantedListingId).forEach(otherSwapProposal -> {
                                    var rejectionNewsletter = rejectionNewsletterBase;
                                    var proposer = swapProposers.get(otherSwapProposal.getProposalId());
                                    var otherProposeSwapToTimetabler = new ProposeSwapToTimetabler(new UnwantedTimeslotListing(unwantedSlot, unwantedListingId), otherSwapProposal.getProposedSlot(), proposer);
                                    rejectionNewsletter.addReceiver(proposer);
                                    
                                    try {
                                        getContentManager().fillContent(rejectionNewsletterBase, otherProposeSwapToTimetabler);
                                    }
                                    catch (Codec.CodecException e) {
                                        e.printStackTrace();
                                    }
                                    catch (OntologyException e) {
                                        e.printStackTrace();
                                    }
                                    send(rejectionNewsletter);
                                    System.out.println("Timetabler sent rejections for delisted slot");
                                    
                                    swapProposers.remove(swapProposal.getProposalId());
                                });
                                
                                //updates tutorial proposals list
                                tutorialSlotSwapProposals.remove(unwantedListingId);
                                
                                //BROADCASTS THAT THE TUTORIAL IS NO LONGER ON OFFER
                                studentAgents.forEach((studentAgent, student) -> {
                                    var broadcast = new ACLMessage(ACLMessage.INFORM);
                                    broadcast.setLanguage(codec.getName());
                                    broadcast.setOntology(ontology.getName());
                                    broadcast.addReceiver(studentAgent);
                                    broadcast.setConversationId("delisted-slot");
                                    
                                    var isNoLongerOnOffer = new IsDelisted();
                                    unwantedTimeslotListing.setUnwantedListingId(unwantedListingId);
                                    unwantedTimeslotListing.setTutorialSlot(unwantedTutorialSlots.get(unwantedListingId));
                                    isNoLongerOnOffer.setUnwantedTimeslotListing(new UnwantedTimeslotListing(unwantedTutorialSlots.get(unwantedListingId), unwantedListingId));
                                    
                                    try {
                                        // Let JADE convert from Java objects to string
                                        getContentManager().fillContent(broadcast, isNoLongerOnOffer);
                                        
                                        send(broadcast);
                                        System.out.println("Notified that " + isNoLongerOnOffer.getUnwantedTimeslotListing().getTutorialSlot().getTimeslotId() + " is now unavailable ");
                                        
                                    }
                                    catch (Codec.CodecException ce) {
                                        ce.printStackTrace();
                                    }
                                    catch (OntologyException oe) {
                                        oe.printStackTrace();
                                    }
                                });
                                
                                tutorialSlotStudentIdsMap.put(swapProposal.getProposedSlot(), unwantedSlotStudentIds);
                                tutorialSlotStudentIdsMap.put(swapProposal.getProposedSlot(), unwantedSlotStudentIds);
                                
                                unwantedTutorialSlots.remove(unwantedListingId);
                                unwantedTutorialOwners.remove(unwantedListingId);
                            }
                            else {
                                throw new Exception("Soft-swap consistency check failed");
                            }
                        }
                        else if (proposalResultResponse.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                            
                            var rejectionLetter = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            rejectionLetter.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                            rejectionLetter.setLanguage(codec.getName());
                            rejectionLetter.setOntology(ontology.getName());
                            rejectionLetter.setConversationId("timeslot-swap");
                            
                            var proposer = swapProposers.get(swapProposal.getProposalId());
                            var proposeSwapToTimetabler = new ProposeSwapToTimetabler(swapProposal, proposer);
                            proposeSwapToTimetabler.setProposer(proposer);
                            
                            rejectionLetter.addReceiver(proposer);
                            
                            try {
                                getContentManager().fillContent(rejectionLetter, proposeSwapToTimetabler);
                                send(rejectionLetter);
                                System.out.println("Timetabler sent proposal rejection");
                                
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
    
    //UTILITY//
    private class UtilityRegistrationServer extends OneShotBehaviour
    {
        @Override
        public void action()
        {
            ACLMessage registration = new ACLMessage(ACLMessage.INFORM);
            registration.addReceiver(utilityAgent);
            //send notif to utilityAgent to register
            registration.setConversationId("register-utility");
            registration.setContent("timetabler");
            registration.setLanguage(codec.getName());
            registration.setOntology(ontology.getName());
            
            send(registration);
            
            //receive response
            var mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
            var reply = myAgent.receive(mt);
            
            if (reply != null && reply.getConversationId().equals("register-utility")) {
                System.out.println("Timetabler registered with UtilityAgent");
                
            }
            else {
                block();
            }
        }
    }
    
    private class EndListener extends CyclicBehaviour
    {
        
        @Override
        public void action() {
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("end"));
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender().getName().equals("utilityAgent")) {
                timeSwapBehaviourEnded = System.currentTimeMillis();
                var behaviourTimeSecs = (timeSwapBehaviourEnded - timeSwapBehaviourStarted) / 1000;
                System.out.println("Swap Behaviour ran for: " + behaviourTimeSecs + " seconds");
                
                var timemsg = new ACLMessage(ACLMessage.INFORM);
                timemsg.addReceiver(utilityAgent);
                //send matric to utilityAgent to register
                timemsg.setConversationId("ending");
                timemsg.setLanguage(codec.getName());
                timemsg.setOntology(ontology.getName());
                timemsg.setContent(Long.toString(timeSwapBehaviourStarted));
                send(timemsg);
                
                takeDown();
            }
            else {
//                System.out.println("Unknown/null message received");
                block();
            }
            
        }
        
    }
    
    public boolean SoftSwapCheckPassed(ArrayList<Long> unwantedSlotStudentIds, TutorialSlot
            unwantedSlot, ArrayList<Long> proposedSlotStudentIds, TutorialSlot proposedSlot)
    {
        
        //double checks for duplicates
        var softSwapDuplicates = tutorialSlotStudentIdsMap.get(unwantedSlotStudentIds.stream().distinct().filter(proposedSlotStudentIds::contains)
                                                                                     .collect(Collectors.toList()));
        assert softSwapDuplicates.size() < 1 : "Duplicates found in proposed swapped assignments of " + unwantedSlot.getTimeslotId() + ", " + proposedSlot.getTimeslotId();
        //?????
        return softSwapDuplicates.size() < 1;
    }
    
    //by nature of how the initial assignments are done and how they are modified there should implicitly be no constraint violations but here's a test anyway
    public boolean CheckHardConstraintsPassed() {
        boolean failed;
        //goes through all the tutorial assignments and looks for duplicates
        for (Module module : modules) {
            for (int i = 0; i < module.getTutorials().size(); i++) {
                var tutorialSlotI = module.getTutorials().get(i).getTutorialSlot();
                
                for (int j = 0; j < module.getTutorials().size(); j++) {
                    if (i != j) {
                        var tutorialSlotJ = module.getTutorials().get(j).getTutorialSlot();
                        tutorialSlotStudentIdsMap.get(tutorialSlotI);
                        var duplicates = tutorialSlotStudentIdsMap.get(tutorialSlotI).stream().distinct().filter(tutorialSlotStudentIdsMap.get(tutorialSlotI)::contains)
                                                                  .collect(Collectors.toList());
                        assert duplicates.size() > 0 : "Duplicates found in tutorials " + tutorialSlotI.getTimeslotId() + ", " + tutorialSlotJ.getTimeslotId();
                        if (duplicates.size() > 0) {
                            return false;
                        }
                    }
                }
            }
        }
        
        for (Student student : students) {
            
            var moduleIds = student.getModuleIds();
            for (int i = 0, moduleIdsSize = student.getModuleIds().size(); i < moduleIdsSize; i++) {
                String moduleId = moduleIds.get(i);
                var count = 0;
                for (Map.Entry<TutorialSlot, ArrayList<Long>> tutorialSlotArrayListEntry : tutorialSlotStudentIdsMap.entrySet()) {
                    TutorialSlot tutorialSlot = tutorialSlotArrayListEntry.getKey();
                    if (tutorialSlot.getModuleId() == moduleId) {
                        count++;
                    }
                }
                assert count > 0 : "Module and assignedTutorial mismatch: Enrolled " + moduleId + " has " + count + " corresponding tutorials";
                if (count > 0) {
                    return false;
                }
            }
        }
        
        return true;
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

