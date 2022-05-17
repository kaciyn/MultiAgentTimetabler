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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    //listingId,ProposedSwapOffers
    private HashMap<Long, ArrayList<SwapProposal>> tutorialSwapProposals;
    
    //proposalId,studentAID
    private HashMap<Long, AID> swapProposers;
    
    private long timeSwapBehaviourEnded;
    private long timeSwapBehaviourStarted;
    
    private AID utilityAgent;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        
        tutorialSlotStudentIdsMap = new HashMap<>();
        studentAgents = new HashMap<>();
        unwantedTutorialSlots = new HashMap<>();
        tutorialSwapProposals = new HashMap<>();
        unwantedTutorialOwners = new HashMap<>();
        swapProposers = new HashMap<>();
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
                myAgent.addBehaviour(new UtilityRegistrationServer());
                myAgent.addBehaviour(new EndListener());
                
            }
        });
        
        System.out.println("Waiting for student agents' registration...");
        addBehaviour(new StudentRegistrationReceiver());
        
        addBehaviour(new SwapServerBehaviour());
        
        addBehaviour(new UtilityRegistrationServer());
        
    }
    
    //registers student agents and sends them their initial tutorial assignments, links student with aid
    private class StudentRegistrationReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getConversationId().equals("register")) {
                
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
                
                var isAssignedTo = new IsAssignedTo();
                isAssignedTo.setAttendingStudentAID(newStudentAID);
                isAssignedTo.setTutorialSlots((ArrayList<TutorialSlot>) assignedTutorialSlots);
                
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
            else {
//                if (msg != null) {
//                    System.out.println("Unknown message received");
//
//                    System.out.println(msg.getContent());
//                }
                block();
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
            addSubBehaviour(new UnwantedSlotReceiver());
            addSubBehaviour(new DelistRequestReceiver());
            
            addSubBehaviour(new SwapOfferReceiver());
            addSubBehaviour(new SwapProposalResponseReceiver());
            timeSwapBehaviourStarted = System.currentTimeMillis();
            
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
                
                if (msg.getConversationId().equals("list-unwanted-slot")) {
                    ContentElement contentElement;
//                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    try {
                        contentElement = getContentManager().extractContent(msg);
                        
                        if (contentElement instanceof Action) {
                            var action = ((Action) contentElement).getAction();
                            
                            if (action instanceof ListUnwantedSlot) {
                                var listAdvertisedSlot = (ListUnwantedSlot) action;
                                
                                //checks the agent isn't trying to advertise someone else's slot
                                if (listAdvertisedSlot.getRequestingStudentAgent().equals(msg.getSender())) {
                                    //creates an id to reference the unwanted slot offer so the offering student's identity is not revealed
                                    var unwantedListingId = Long.valueOf(listAdvertisedSlot.getUnwantedTutorialSlot().getTimeslotId() + ThreadLocalRandom.current().nextInt());
                                   
                                    if (unwantedListingId<0){
                                        var sdkjf=445+4;
                                    }
                                    var unwantedSlot = listAdvertisedSlot.getUnwantedTutorialSlot();
                                    
                                    //lists in unwanted slots
                                    unwantedTutorialSlots.put(unwantedListingId, unwantedSlot);
                                    //creates proposal list for slot
                                    tutorialSwapProposals.put(unwantedListingId, new ArrayList<>());
                                    //note listing owner
                                    unwantedTutorialOwners.put(unwantedListingId, listAdvertisedSlot.getRequestingStudentAgent());
                                    
                                    ////respond to requester
                                    reply.setPerformative(ACLMessage.INFORM);
                                    reply.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                                    reply.setLanguage(codec.getName());
                                    reply.setOntology(ontology.getName());
                                    reply.addReceiver(listAdvertisedSlot.getRequestingStudentAgent());
                                    System.out.println("Timetabler confirming receipt of unwanted slot:" + unwantedSlot.getTimeslotId() + " from agent " + listAdvertisedSlot.getRequestingStudentAgent());
                                    
                                    var unwantedTimeslotListing = new UnwantedTimeslotListing();
                                    unwantedTimeslotListing.setUnwantedListingId(unwantedListingId);
                                    
                                    unwantedTimeslotListing.setTutorialSlot(unwantedSlot);
                                    
                                    var isListed = new IsListed();
                                    
                                    isListed.setRequestingStudent(listAdvertisedSlot.getRequestingStudentAgent());
                                    isListed.setUnwantedTimeslotListing(unwantedTimeslotListing);
                                    
                                    getContentManager().fillContent(reply, isListed);
                                    
                                    send(reply);
                                    
                                    //put on offer
                                    var isOnOffer = new IsOnOffer();
                                    isOnOffer.setUnwantedTimeslotListing(unwantedTimeslotListing);
                                    
                                    var broadcast = new ACLMessage(ACLMessage.CFP);
                                    broadcast.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                    broadcast.setLanguage(codec.getName());
                                    broadcast.setOntology(ontology.getName());
                                    broadcast.setConversationId("unwanted-slot");
                                    
                                    try {
                                        // Let JADE convert from Java objects to string
                                        getContentManager().fillContent(broadcast, isOnOffer);
                                        System.out.println("Broadcasting unwanted tutorial " + isOnOffer.getUnwantedTimeslotListing().getUnwantedListingId()
//                                                   + " to Student " + studentAgent.getName()
                                        );
                                    }
                                    catch (Codec.CodecException ce) {
                                        ce.printStackTrace();
                                    }
                                    catch (OntologyException oe) {
                                        oe.printStackTrace();
                                    }
                                    
                                    //THIS IS THE OLD UnwantedSlotListBroadcaster method, was refactored out before but the arg passing thing, found out i can MAYBE? add multiple receivers for messages so here we go
                                    //this should be ok for normal student numbers but may face scaling issues if really stupid numbers are plugged in
                                    studentAgents.forEach((studentAgent, student) -> {
                                        broadcast.addReceiver(studentAgent);
                                        send(broadcast);
                                        
                                    });
                                    
                                }
                            }
                        }
                        else {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("Invalid request");
                            block();
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
            else {
                block();
            }
        }
        
    }
    
    private class DelistRequestReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchConversationId("delist-advertised-slot"),
                                                                         MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST)),
                                                     MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            
            ACLMessage msg = receive(mt);
            if (msg != null) {
                var studentAID = msg.getSender();
                
                var reply = msg.createReply();
                reply.setLanguage(codec.getName());
                reply.setOntology(ontology.getName());
                reply.setConversationId("delist-advertised-slot");
                reply.addReceiver(studentAID);
                
                if (msg.getConversationId().equals("delist-advertised-slot")) {
                    ContentElement contentElement;
//                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    try {
                        contentElement = getContentManager().extractContent(msg);
                        
                        if (contentElement instanceof Action) {
                            var action = ((Action) contentElement).getAction();
                            
                            if (action instanceof DelistUnwantedSlot) {
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
                                    
                                    send(reply);
                                    
                                    var broadcast = new ACLMessage(ACLMessage.INFORM);
                                    broadcast.setLanguage(codec.getName());
                                    broadcast.setOntology(ontology.getName());
                                    broadcast.setConversationId("taken-slot");
                                    
                                    var isNoLongerOnOffer = new IsNoLongerOnOffer();
                                    isNoLongerOnOffer.setUnwantedTimeslotListing(slotListingToDelist);
                                    
                                    //this should be ok for normal student numbers but may face scaling issues if really stupid numbers are plugged in
                                    studentAgents.forEach((studentAgent, student) -> {
                                        broadcast.addReceiver(studentAgent);
                                    });
                                    
                                    try {
                                        // Let JADE convert from Java objects to string
                                        getContentManager().fillContent(broadcast, isNoLongerOnOffer);
                                        send(broadcast);
                                        
                                        System.out.println("Broadcast delisted tutorial slot " + isNoLongerOnOffer.getUnwantedTimeslotListing().getUnwantedListingId()
//                                                   + " to Student " + studentAgent.getName()
                                        );
                                        
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
                        else {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("Invalid request");
                            block();
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
            else {
                block();
            }
        }
    }
    
    private class SwapOfferReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            var mt = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
            
            var proposalMsg = receive(mt);
            if (proposalMsg != null && proposalMsg.getConversationId().equals("offer-timeslot-swap")) {
                
                var proposalReply = proposalMsg.createReply();
                //UNSURE IF THIS HAS TO BE RE-SET?
                proposalReply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);

//                System.out.println(proposalMsg.getContent()); //print out the message content in SL
                
                try {
                    var contentElement = getContentManager().extractContent(proposalMsg);
                    
                    if (contentElement instanceof Action) {
                        var action = ((Action) contentElement).getAction();
                        
                        if (action instanceof ProposeSwapToTimetabler) {
                            var proposeSwapToTimetabler = (ProposeSwapToTimetabler) action;
                            var proposedSwap = proposeSwapToTimetabler.getSwapProposal();
                            
                            var unwantedListingId = proposedSwap.getUnwantedListingId();
                            var proposedTutorialSlot = proposedSwap.getProposedSlot();
                            var proposingStudent = proposalMsg.getSender();
                            
                            //check if the module matches, will also catch if it's been removed
                            if (proposedTutorialSlot.getModuleId() != unwantedTutorialSlots.get(unwantedListingId).getModuleId() || proposedSwap.getUnwantedListingId() != unwantedListingId) {
                                proposalReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                proposalReply.setContent("REJECTED SWAP PROPOSAL: MODULE MISMATCH");
                                
                            }
                            else {
                                if (proposeSwapToTimetabler.getProposer().equals(proposalMsg.getSender())) {
                                    //creates an id to reference the proposal offer so the proposing student's identity is not revealed
                                    var proposalId = Long.valueOf(proposedTutorialSlot.getTimeslotId() + ThreadLocalRandom.current().nextInt());
                                    
                                    //add to proposers list
                                    swapProposers.put(proposalId, proposingStudent);
                                    
                                    var proposedSwapOffer = new SwapProposal();
                                    
                                    proposedSwapOffer.setProposalId(proposalId);
                                    proposedSwapOffer.setUnwantedListingId(unwantedListingId);
                                    proposedSwapOffer.setProposedSlot(proposedTutorialSlot);
                                    
                                    //adds to proposal list
                                    var proposalsEntry = tutorialSwapProposals.get(proposalId);
                                    ArrayList<SwapProposal> offers;
                                    if (proposalsEntry == null) {
                                        offers = new ArrayList<>();
                                    }
                                    else {
                                        offers = tutorialSwapProposals.get(proposalId);
                                    }
                                    offers.add(proposedSwapOffer);
                                    tutorialSwapProposals.put(proposalId, offers);
                                    
                                    //INFORM proposer of proposal
                                    var swapProposal = new SwapProposal();
                                    swapProposal.setUnwantedListingId(unwantedListingId);
                                    swapProposal.setProposedSlot(proposedTutorialSlot);
                                    swapProposal.setProposalId(proposalId);
                                    
                                    var isProposed = new IsProposed();
                                    isProposed.setSwapProposal(swapProposal);
                                    
                                    proposalReply.setPerformative(ACLMessage.INFORM);
                                    proposalReply.setLanguage(codec.getName());
                                    proposalReply.setOntology(ontology.getName());
                                    
                                    getContentManager().fillContent(proposalReply, isProposed);
                                    
                                    send(proposalReply);
                                    
                                    //PROPOSE to requester
                                    var unwantedTutorialSlot = unwantedTutorialSlots.get(unwantedListingId);
                                    
                                    var requestingStudentAgent = unwantedTutorialOwners.get(unwantedListingId);
                                    
                                    var swapProposalWithId = swapProposal;
                                    swapProposalWithId.setProposalId(proposalId);
                                    
                                    var proposeSwapToStudent = new ProposeSwapToStudent();
                                    proposeSwapToStudent.setSwapProposal(swapProposalWithId);
                                    proposeSwapToStudent.setStudent(requestingStudentAgent);
                                    
                                    var proposalOfferMsg = new ACLMessage(ACLMessage.PROPOSE);
                                    proposalOfferMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                    proposalOfferMsg.addReceiver(requestingStudentAgent);
                                    proposalOfferMsg.setLanguage(codec.getName());
                                    proposalOfferMsg.setOntology(ontology.getName());
                                    proposalOfferMsg.setConversationId("propose-timeslot-swap");
                                    
                                    var proposal = new Action();
                                    proposal.setAction(proposeSwapToStudent);
                                    proposal.setActor(requestingStudentAgent);
                                    
                                    getContentManager().fillContent(proposalOfferMsg, proposal);
                                    
                                    send(proposalMsg);
                                }
                            }
                        }
                    }
                    
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
    
    private class SwapProposalResponseReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            MessageTemplate proposalResponseMt = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                                                                                         MessageTemplate.MatchConversationId("propose-timeslot-swap")), MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL), MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)));
            
            var proposalResultResponse = receive(proposalResponseMt);
            
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
                        var perf = proposalResultResponse.getPerformative();
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
                                var proposeSwapToTimetabler = new ProposeSwapToTimetabler();
                                proposeSwapToTimetabler.setSwapProposal(swapProposal);
                                proposeSwapToTimetabler.setProposer(proposingStudentAID);
                                
                                var acceptProposalForward = new ACLMessage((ACLMessage.ACCEPT_PROPOSAL));
                                acceptProposalForward.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                acceptProposalForward.addReceiver(proposingStudentAID);
                                acceptProposalForward.setConversationId("propose-timeslot-swap");
                                acceptProposalForward.setLanguage(codec.getName());
                                acceptProposalForward.setOntology(ontology.getName());
                                //shouldn't cause an issue since it's directly replying to the accept message
                                
                                //todo this is weird i don't think it should be the swap result but whatever the proposer sent instead
                                getContentManager().fillContent(acceptProposalForward, isSwapResult);
                                
                                send(acceptProposalForward);
                                
                                //swap result inform to both students
                                var swapConfirmation = proposalResultResponse.createReply();
                                swapConfirmation.setPerformative(ACLMessage.INFORM);
                                swapConfirmation.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                swapConfirmation.setConversationId("propose-timeslot-swap");
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
                                rejectionNewsletterBase.setConversationId("propose-timeslot-swap");
                                
                                //rejects other offers
                                //i don't particularly like this implementation
                                tutorialSwapProposals.get(unwantedListingId).forEach(otherSwapProposal -> {
                                    var rejectionNewsletter = rejectionNewsletterBase;
                                    var proposer = swapProposers.get(otherSwapProposal.getProposalId());
                                    var otherProposeSwapToTimetabler = new ProposeSwapToTimetabler();
                                    otherProposeSwapToTimetabler.setSwapProposal(otherSwapProposal);
                                    otherProposeSwapToTimetabler.setProposer(proposer);
                                    
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
                                    
                                    swapProposers.remove(swapProposal.getProposalId());
                                });
                                
                                //updates tutorial proposals list
                                tutorialSwapProposals.remove(unwantedListingId);
                                
                                //BROADCASTS THAT THE TUTORIAL IS NO LONGER ON OFFER
                                studentAgents.forEach((studentAgent, student) -> {
                                    var broadcast = new ACLMessage(ACLMessage.INFORM);
                                    broadcast.setLanguage(codec.getName());
                                    broadcast.setOntology(ontology.getName());
                                    broadcast.addReceiver(studentAgent);
                                    broadcast.setConversationId("taken-slot");
                                    
                                    var isNoLongerOnOffer = new IsNoLongerOnOffer();
                                    var unwantedTimeslotListing = new UnwantedTimeslotListing();
                                    unwantedTimeslotListing.setUnwantedListingId(unwantedListingId);
                                    unwantedTimeslotListing.setTutorialSlot(unwantedTutorialSlots.get(unwantedListingId));
                                    isNoLongerOnOffer.setUnwantedTimeslotListing(new UnwantedTimeslotListing());
                                    
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
                            rejectionLetter.setConversationId("propose-timeslot-swap");
                            
                            var proposer = swapProposers.get(swapProposal.getProposalId());
                            var proposeSwapToTimetabler = new ProposeSwapToTimetabler();
                            proposeSwapToTimetabler.setSwapProposal(swapProposal);
                            proposeSwapToTimetabler.setProposer(proposer);
                            
                            rejectionLetter.addReceiver(proposer);
                            
                            try {
                                getContentManager().fillContent(rejectionLetter, proposeSwapToTimetabler);
                                send(rejectionLetter);
                                
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
    
    public boolean SoftSwapCheckPassed(ArrayList<Long> unwantedSlotStudentIds, TutorialSlot unwantedSlot, ArrayList<Long> proposedSlotStudentIds, TutorialSlot proposedSlot) {
        
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

