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
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TimetablerAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    
    private Timetable timetable;
    private HashMap<AID, Student> studentAgents;
    private ArrayList<Student> students;
    //
    private HashMap<Long, IsUnwanted> unwantedTutorials;
    //offerId,tutorial
    private HashMap<Long, Long> tutorialsOnOffer;
    
    private long timeSwapBehaviourEnded;
    private long timeSwapBehaviourStarted;
    
    private AID utilityAgent;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        studentAgents = new HashMap<>();
        unwantedTutorials = new HashMap<>();
        tutorialsOnOffer = new HashMap<>();
        
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
            timetable = (Timetable) args[0];
            
            System.out.println("Initial timetable loaded");
        }
        if (args != null && args.length > 1) {
            students = (ArrayList<Student>) args[1];
            
            System.out.println("Students loaded");
        }
        else {
// Make the agent terminate immediately
            System.out.println("No timetable or students found");
            doDelete();
        }

//        addBehaviour(new TickerBehaviour(this, 10000)
//        {
//            @Override
//            protected void onTick()
//            {  // Search for utility agent
//                var utilTemplate = new DFAgentDescription();
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
//            }
//        });
        
        System.out.println("Waiting for student agents' registration...");
        addBehaviour(new StudentRegistrationReceiver());
        
        addBehaviour(new SwapServerBehaviour());
        
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
                var newStudentMatric = java.lang.Integer.parseInt(msg.getContent());
                var newStudent = students.stream().filter(student -> newStudentMatric == student.getMatriculationNumber()).findFirst().orElse(null);
                
                var reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.addReplyTo(newStudentAID);
                reply.setLanguage(codec.getName());
                reply.setOntology(ontology.getName());
                
                if (newStudent == null) {
                    
                    reply.setContent("Student is not enrolled");
                    
                    System.out.println(newStudentAID.getName() + " not registered ");
                    
                    myAgent.send(reply);
                    block();
                }
                
                if (studentAgents.containsKey(newStudentAID)) {
                    System.out.println(newStudentAID.getName() + " already registered ");
                    
                    myAgent.send(reply);
                    block();
                }
                
                studentAgents.put(newStudentAID, newStudent);
                
                var studentTutorials = newStudent.getTutorialSlots();
                ArrayList<Long> studentTutorialSlots = new ArrayList<Long>();
                studentTutorials.forEach((tutorialSlot) -> {
                    studentTutorialSlots.add(tutorialSlot);
                });
                
                var isAssignedTo = new IsAssignedTo();
                isAssignedTo.setAttendingStudent(newStudent);
                isAssignedTo.setTutorialSlots(studentTutorialSlots);
                
                try {
                    // Let JADE convert from Java objects to string
                    getContentManager().fillContent(reply, isAssignedTo);
                    send(reply);
                    System.out.println(newStudentAID.getName() + " registered ");
                    
                }
                catch (Codec.CodecException ce) {
                    ce.printStackTrace();
                }
                catch (OntologyException oe) {
                    oe.printStackTrace();
                }
            }
            else {
                if (msg != null) {
                    System.out.println("Unknown message received");
                    
                    System.out.println(msg.getContent());
                }
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
            addBehaviour(new UnwantedSlotReceiver());
            addBehaviour(new SwapOfferReceiver());
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
                //TODO CHECK IF THERE ARE OTHER REQUESTS COMING IN BC WE DON'T WANNA JUST REJECT THEM LOL
                //^^WHAT DID I MEAN BY THIS
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
                                
                                unwantedTutorials.put(Long.valueOf(unwantedId), isUnwanted);
                                tutorialsOnOffer.put(Long.valueOf(unwantedId), isUnwanted.getTutorialSlot());
                                //TODO JUST SEND THIS OUT AS AN UPDATE WHICH GETS APPENDED TO THE LIST STUDENTS HAVE
                                reply.setPerformative(ACLMessage.AGREE);
                                reply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                                
                                //todo check this does in fact continue
                                //THIS IS THE OLD UnwantedSlotListBroadcaster method
                                studentAgents.forEach((studentAgent, student) -> {
                                    var broadcast = new ACLMessage(ACLMessage.CFP);
                                    broadcast.setLanguage(codec.getName());
                                    broadcast.setOntology(ontology.getName());
                                    broadcast.addReceiver(studentAgent);
                                    broadcast.setConversationId("unwanted-slot");
                                    //todo -> add module to timeslot too + add checks to ensure each student in the correct amount of tutorials?
                                    
                                    var isOnOffer = new IsOnOffer();
                                    isOnOffer.setUnwantedTutorialSlot(isUnwanted.getTutorialSlot());
                                    isOnOffer.setUnwantedTutorialId((long) unwantedId);
                                    
                                    try {
                                        // Let JADE convert from Java objects to string
                                        getContentManager().fillContent(broadcast, isOnOffer);
                                        System.out.println("Sent unwanted tutorial " + isOnOffer.getUnwantedTutorialSlot() + " to Student " + studentAgent.getName());
                                        
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
    
    private class SwapOfferReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            var mt = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
            
            var offerMsg = receive(mt);
            if (offerMsg != null && offerMsg.getConversationId().equals("offer-timeslot-swap")) {
                
                System.out.println(offerMsg.getContent()); //print out the message content in SL
                
                try {
                    var contentElement = getContentManager().extractContent(offerMsg);
                    
                    if (contentElement instanceof Action) {
                        var action = ((Action) contentElement).getAction();
                        
                        if (action instanceof OfferSwap) {
                            var offerSwap = (OfferSwap) action;
                            var offerId = offerSwap.getOfferId();
                            var offeredTutorialSlot = offerSwap.getOfferedTutorialSlot();
                            
                            var requestedTutorialSlot = tutorialsOnOffer.get(offerId);
                            
                            var requestingStudentAgent = unwantedTutorials.get(offerId).getStudentAID();
                            
                            var proposalMsg = new ACLMessage(ACLMessage.PROPOSE);
                            proposalMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                            proposalMsg.addReceiver(requestingStudentAgent);
                            proposalMsg.setLanguage(codec.getName());
                            proposalMsg.setOntology(ontology.getName());
                            proposalMsg.setConversationId("propose-timeslot-swap");
                            
                            var acceptSwap = new AcceptSwap();
                            var proposal = new Action();
                            proposal.setAction(acceptSwap);
                            proposal.setActor(requestingStudentAgent);
                            acceptSwap.setProposedTutorialSlot(offeredTutorialSlot);
                            acceptSwap.setUnwantedTutorialSlot(requestedTutorialSlot);
                            
                            getContentManager().fillContent(proposalMsg, proposal);
                            
                            send(proposalMsg);
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
    
    private class SwapProposalResponseReceiver extends CyclicBehaviour
    {
        @Override
        public void action() {
            MessageTemplate replyMt = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                                                                         MessageTemplate.MatchConversationId("propose-timeslot-swap")), MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL), MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)));
            
            
            
            var proposalReply = receive(replyMt);
//            while (proposalReply == null) {
//                proposalReply = receive(replyMt);
//            }
    
            if (proposalReply != null) {
                if (proposalReply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                   
                   
                    //offerStudent accept inform
                    proposalReply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    getContentManager().fillContent(offerResultReply, isSwapResult);
                    send(offerResultReply);
            
                    //requestStudent accept inform
                    var swapConfirm = offerMsg.createReply();
                    swapConfirm.setPerformative(ACLMessage.CONFIRM);
                    swapConfirm.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
            
                    getContentManager().fillContent(offerResultReply, isSwapResult);
                    send(swapConfirm);
            
                    System.out.println("Swapped " + isSwapResult.getOfferedTutorialSlot());
            
                    //updates requesting student
                    var requestingStudent = studentAgents.get(requestingStudentAgent);
                    students.remove(requestingStudent);
            
                    requestingStudent.removeTutorialSlot(requestedTutorialSlot);
                    requestingStudent.addTutorialSlot(offeredTutorialSlot);
            
                    students.add(requestingStudent);
                    studentAgents.put(requestingStudentAgent, requestingStudent);
            
                    //updates offering student
                    var offeringStudent = studentAgents.get(offerMsg.getSender());
                    students.remove(offeringStudent);
            
                    offeringStudent.removeTutorialSlot(tutorialsOnOffer.get(offeredTutorialSlot));
                    offeringStudent.addTutorialSlot(requestedTutorialSlot);
            
                    students.add(offeringStudent);
            
                    studentAgents.put(offerMsg.getSender(), offeringStudent);
            
                    //updates tutorials
                    tutorialsOnOffer.remove(offerId);
                    unwantedTutorials.remove(offerId);
            
                    //BROADCASTS THAT THE TUTORIAL IS NO LONGER ON OFFER
                    studentAgents.forEach((studentAgent, student) -> {
                        var broadcast = new ACLMessage(ACLMessage.INFORM);
                        broadcast.setLanguage(codec.getName());
                        broadcast.setOntology(ontology.getName());
                        broadcast.addReceiver(studentAgent);
                        broadcast.setConversationId("taken-slot");
                        //todo -> add module to timeslot too + add checks to ensure each student in the correct amount of tutorials?
                
                        var isNoLongerOnOffer = new IsNoLongerOnOffer();
                        isNoLongerOnOffer.setUnavailableTutorialId(offerId);
                
                        try {
                            // Let JADE convert from Java objects to string
                            getContentManager().fillContent(broadcast, isNoLongerOnOffer);
                    
                            myAgent.send(broadcast);
                            System.out.println("Sent unavailable tutorial " + isNoLongerOnOffer.getUnavailableTutorialId() + " to Student " + studentAgent.getName());
                    
                        }
                        catch (Codec.CodecException ce) {
                            ce.printStackTrace();
                        }
                        catch (OntologyException oe) {
                            oe.printStackTrace();
                        }
                    });
            
                }
                if (offerMsg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    offerResultReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    send(offerResultReply);
            
                }
        
            }
            else {
                block();
            }
            
            var offerResultReply = offerMsg.createReply();
            
            var isSwapResult = new IsSwapResult();
            
            isSwapResult.setRequestedTutorialSlot(requestedTutorialSlot);
            isSwapResult.setOfferedTutorialSlot(offeredTutorialSlot);
            
            if (proposalReply != null) {
                if (offerMsg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    //offerStudent accept inform
                    offerResultReply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    getContentManager().fillContent(offerResultReply, isSwapResult);
                    send(offerResultReply);
                    
                    //requestStudent accept inform
                    var swapConfirm = offerMsg.createReply();
                    swapConfirm.setPerformative(ACLMessage.CONFIRM);
                    swapConfirm.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                    
                    getContentManager().fillContent(offerResultReply, isSwapResult);
                    send(swapConfirm);
                    
                    System.out.println("Swapped " + isSwapResult.getOfferedTutorialSlot());
                    
                    //updates requesting student
                    var requestingStudent = studentAgents.get(requestingStudentAgent);
                    students.remove(requestingStudent);
                    
                    requestingStudent.removeTutorialSlot(requestedTutorialSlot);
                    requestingStudent.addTutorialSlot(offeredTutorialSlot);
                    
                    students.add(requestingStudent);
                    studentAgents.put(requestingStudentAgent, requestingStudent);
                    
                    //updates offering student
                    var offeringStudent = studentAgents.get(offerMsg.getSender());
                    students.remove(offeringStudent);
                    
                    offeringStudent.removeTutorialSlot(tutorialsOnOffer.get(offeredTutorialSlot));
                    offeringStudent.addTutorialSlot(requestedTutorialSlot);
                    
                    students.add(offeringStudent);
                    
                    studentAgents.put(offerMsg.getSender(), offeringStudent);
                    
                    //updates tutorials
                    tutorialsOnOffer.remove(offerId);
                    unwantedTutorials.remove(offerId);
                    
                    //BROADCASTS THAT THE TUTORIAL IS NO LONGER ON OFFER
                    studentAgents.forEach((studentAgent, student) -> {
                        var broadcast = new ACLMessage(ACLMessage.INFORM);
                        broadcast.setLanguage(codec.getName());
                        broadcast.setOntology(ontology.getName());
                        broadcast.addReceiver(studentAgent);
                        broadcast.setConversationId("taken-slot");
                        //todo -> add module to timeslot too + add checks to ensure each student in the correct amount of tutorials?
                        
                        var isNoLongerOnOffer = new IsNoLongerOnOffer();
                        isNoLongerOnOffer.setUnavailableTutorialId(offerId);
                        
                        try {
                            // Let JADE convert from Java objects to string
                            getContentManager().fillContent(broadcast, isNoLongerOnOffer);
                            
                            myAgent.send(broadcast);
                            System.out.println("Sent unavailable tutorial " + isNoLongerOnOffer.getUnavailableTutorialId() + " to Student " + studentAgent.getName());
                            
                        }
                        catch (Codec.CodecException ce) {
                            ce.printStackTrace();
                        }
                        catch (OntologyException oe) {
                            oe.printStackTrace();
                        }
                    });
                    
                }
                if (offerMsg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    offerResultReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    send(offerResultReply);
                    
                }
                
            }
            else {
                block();
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
                
            }
        }
        
        private class EndListener extends CyclicBehaviour
        {
            
            @Override
            public void action() {
                var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("end"));
                var msg = myAgent.receive(mt);
                
                if (msg != null && msg.getSender().getName() == "utilityAgent") {
                    timeSwapBehaviourEnded = System.currentTimeMillis();
                    var behaviourTimeSecs = timeSwapBehaviourEnded - timeSwapBehaviourStarted / 1000;
                    System.out.println("Swap Behaviour ran for: " + behaviourTimeSecs + " seconds");
                    
                    var timemsg = new ACLMessage(ACLMessage.INFORM);
                    timemsg.addReceiver(utilityAgent);
                    //send matric to utilityAgent to register
                    timemsg.setConversationId("end");
                    
                    timemsg.setContent(Long.toString(behaviourTimeSecs));
                    myAgent.send(timemsg);
                    
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
            
            System.out.println("Timetabler " + getAID().getName() + " terminating.");
        }
    }


