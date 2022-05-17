package Agents;

import Ontology.Elements.AreCurrentFor;
import Ontology.MultiAgentTimetablerOntology;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UtilityAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    private ArrayList<AID> studentAgents;
    private AID timetabler;
    private int numberOfStudents;
    
    //keeping in separate ones to allow for stream reduce
    private HashMap<AID, Long> studentUtilities;
    private HashMap<AID, Long> studentMessagesSent;
    
    private long maxRunTime;
    
    private long utilityPollPeriod;
    
    private long totalSystemUtility;
    
    private float averageSystemUtility;
    
    private float lowAverageUtilityThreshold;
    private long lowAverageUtilityThresholdTimeReached;
    private float mediumAverageUtilityThreshold;
    private long mediumAverageUtilityThresholdTimeReached;
    
    private float finalAverageUtilityThreshold;
    private long finalAverageUtilityThresholdTimeReached;
    
    private long bestStudentUtility;
    private long worstStudentUtility;
    
    private long totalMessagesSent;
    private float averageMessagesSent;
    
    private long initialTotalUtility;
    
    private long timeSwapBehaviourStarted;
    
    private static ArrayList<Long> runConfig;
    
    private static ArrayList<String[]> utilityPolls;
    
    private String runTime;
    
    protected void setup()
    {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        studentAgents = new ArrayList<>();
        
        // Register the the UtilityAgent in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("utilityAgent");
        sd.setName("utilityAgent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            runConfig = (ArrayList<Long>) args[0];
            
            System.out.println("Run config loaded");
        }
        else {
// Make the agent terminate immediately
            System.out.println("No utilityThreshold found");
            doDelete();
        }
        utilityPolls=new ArrayList<>();
        bestStudentUtility=-100000;
        worstStudentUtility=100000;
        
        var numberOfModules = runConfig.get(0);
        var tutorialGroupsPerModule = runConfig.get(1);
        
        var numberOfStudents = runConfig.get(2);
        
        var modulesPerStudent = runConfig.get(3);
        
        //student tuning
        var highMinimumSwapUtilityGain = runConfig.get(4);
        var mediumMinimumSwapUtilityGain = runConfig.get(5);
        var lowMinimumSwapUtilityGain = runConfig.get(6);
        var mediumUtilityThreshold = runConfig.get(7);
        var highUtilityThreshold = runConfig.get(8);
        var unwantedSlotCheckPeriod = runConfig.get(9);

//     utility tuning
        utilityPollPeriod = runConfig.get(10);
        
        lowAverageUtilityThreshold = runConfig.get(11);
        
        mediumAverageUtilityThreshold = runConfig.get(12);
        
        finalAverageUtilityThreshold = runConfig.get(13);
        
        var maxRunTimeSecs = runConfig.get(14);
        maxRunTime = maxRunTimeSecs * 1000;
        
        studentUtilities = new HashMap<>();
        studentMessagesSent = new HashMap<>();
        
        System.out.println("Waiting for student agents' registration...");
        addBehaviour(new RegistrationReceiver());
        
        addBehaviour(new WakerBehaviour(this, 10000)
        {
            @Override
            protected void onWake() {
                super.onWake();
                addBehaviour(new StatsReceiver());
                
                addBehaviour(new UtilityPoller(this.myAgent, utilityPollPeriod));
                
                addBehaviour(new ThresholdReached());
            }
        });
        
        addBehaviour(new WakerBehaviour(this, 30000)
        {
            @Override
            protected void onWake() {
                super.onWake();
                addBehaviour(new AgentShutdownListener());
                
            }
        });
        
    }
    
    //registers agents
    // WOULD HAVE BEEN NICE TO: sends request inform if
    //but going to do a ticker to poll once every bit instead for simplicity
    private class RegistrationReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getConversationId().equals("register-utility")) {
                if (msg.getContent() == "timetabler") {
                    timetabler = msg.getSender();
                }
                else {
                    studentAgents.add(msg.getSender());
                    
                    var reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.addReceiver(msg.getSender());
                    reply.setLanguage(codec.getName());
                    reply.setOntology(ontology.getName());
                    reply.setConversationId("register");
                    reply.setContent("true");
                    send(reply);
                    
                    numberOfStudents = studentAgents.size();
                    
                }
            }
            else {
//                System.out.println("Unknown/null message received");
                block();
            }
            
        }
        
    }
    
    public class UtilityPoller extends TickerBehaviour
    {
        public UtilityPoller(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            studentAgents.forEach(studentAgent -> {
                var msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(studentAgent);
                msg.setConversationId("current-stats");
                msg.setLanguage(codec.getName());
                msg.setOntology(ontology.getName());
                msg.setContent("Please report your current stats");
                send(msg);
            });
            
            CalculateTotalStats();
    
            System.out.println("Current total system utility = " + totalSystemUtility);
            System.out.println("Current average system utility = " + averageSystemUtility);
            System.out.println("Current total messages sent = " + totalMessagesSent);
            System.out.println("Current average messages sent = " + averageMessagesSent);
            
        }
        
    }
    
    public class StatsReceiver extends CyclicBehaviour
    {
        
        @Override
        public void action() {
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("current-stats"));
            var msg = myAgent.receive(mt);
            
            if (msg != null) {
                try {
                    ContentElement contentElement = null;

//                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof AreCurrentFor) {
                        var areCurrentFor = (AreCurrentFor) contentElement;
                        var student = msg.getSender();
                        var stats = areCurrentFor.getStudentStats();
                        var utility = stats.getCurrentTotalUtility();
                        var messagesSent = stats.getMessagesSent();
                        
                        studentUtilities.put(student, utility);
                        studentMessagesSent.put(student, messagesSent);
                        
                        if (stats.isInitialStats()) {
                            initialTotalUtility += utility;
                        }
                        //could add extra messages sent here but probably not entirely necessary
                        if (stats.isFinalStats()) {
                            if (utility < worstStudentUtility) {
                                worstStudentUtility = utility;
                            }
                            if (utility > bestStudentUtility) {
                                bestStudentUtility = utility;
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
                //this might be too much of a chaos
                CalculateTotalStats();
            }
            else {
//                System.out.println("Unknown/null message received");
//                System.out.println("Sender:" + msg.getSender());
                
                block();
            }
            
        }
    }
    
    public class ThresholdReached extends OneShotBehaviour
    {
        @Override
        public void action() {
            
            myAgent.addBehaviour(new UtilityThreshold0Reached(this.myAgent, 2000));
            myAgent.addBehaviour(new UtilityThreshold1Reached(this.myAgent, 2000));
            myAgent.addBehaviour(new FinalUtilityThresholdReached(this.myAgent, 5000));
            myAgent.addBehaviour(new MaxRunTimeReached(this.myAgent, maxRunTime));
            
        }
    }
    
    public class UtilityThreshold0Reached extends TickerBehaviour
    {
        public UtilityThreshold0Reached(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            if (averageSystemUtility >= lowAverageUtilityThreshold) {
                lowAverageUtilityThresholdTimeReached = System.currentTimeMillis();
            }
        }
    }
    
    public class UtilityThreshold1Reached extends TickerBehaviour
    {
        public UtilityThreshold1Reached(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            if (averageSystemUtility >= mediumAverageUtilityThreshold) {
                mediumAverageUtilityThresholdTimeReached = System.currentTimeMillis();
            }
        }
    }
    
    public class FinalUtilityThresholdReached extends TickerBehaviour
    {
        public FinalUtilityThresholdReached(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            if (averageSystemUtility >= finalAverageUtilityThreshold) {
                finalAverageUtilityThresholdTimeReached = System.currentTimeMillis();
                
                System.out.println("Final average utility threshold at" + averageSystemUtility + " reached, notifying shutdown.");
                
                myAgent.addBehaviour(new NotifyEnd());
            }
        }
    }
    
    public class MaxRunTimeReached extends WakerBehaviour
    {
        
        public MaxRunTimeReached(Agent a, long timeout) {
            super(a, timeout);
        }
        
        @Override
        public void onWake() {
            System.out.println("Max run time at" + maxRunTime + " reached, notifying shutdown.");
            
            myAgent.addBehaviour(new NotifyEnd());
            
        }
    }
    
    public class NotifyEnd extends OneShotBehaviour
    {
        @Override
        public void action() {
            var endMsg = new ACLMessage(ACLMessage.INFORM);
            endMsg.setConversationId("end");
            
            studentAgents.forEach(studentAgent -> {
                
                endMsg.addReceiver(studentAgent);
                endMsg.setLanguage(codec.getName());
                endMsg.setOntology(ontology.getName());
                send(endMsg);
//
//                var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("end"));
//                var msg = myAgent.receive(mt);
//
//                if (msg != null && msg.getSender() == studentAgent) {
//                    var studentUtility = Float.parseFloat(msg.getContent());
//                    studentUtilities.put(studentAgent, studentUtility);
//                }
//
            });
            
            endMsg.addReceiver(timetabler);
            send(endMsg);
            
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("end"));
            var msg = myAgent.receive(mt);
            
            if (msg != null && msg.getSender() == timetabler) {
                timeSwapBehaviourStarted = Long.parseLong(msg.getContent());

//                System.out.println("Swap Behaviour ran for: " + timeSwapBehaviourEnded + " seconds");
                
            }
            else {
                block();
            }
            CalculateTotalStats();
            
            //would be better to print this to a file
            System.out.println("Total system Utility: " + totalSystemUtility);
            System.out.println("Average system Utility: " + averageSystemUtility);
            
            System.out.println("Initial system Utility: " + initialTotalUtility);
            var initAvgUtil = (float) initialTotalUtility / (float) numberOfStudents;
            System.out.println("Initial average system Utility: " + initAvgUtil);
            
            System.out.println("Net Total system Utility change: " + (totalSystemUtility - initialTotalUtility));
            System.out.println("Net Average system Utility change: " + (averageSystemUtility - initAvgUtil));
            
            System.out.println("Total messages sent: " + totalMessagesSent);
            System.out.println("Average messages sent: " + averageMessagesSent);
            
        }
    }
    
    private class AgentShutdownListener extends CyclicBehaviour
    {
        
        @Override
        public void action() {
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("ending"));
            var msg = myAgent.receive(mt);
            
            if (msg != null && studentAgents.contains(msg.getSender())) {
                
                studentAgents.remove(msg.getSender());
                
                System.out.println("Student " + msg.getSender() + " shutting down and removed from utility agent.");
                
            }
            else if (msg != null && msg.getSender() == timetabler) {
                runTime = msg.getContent();
                //crude but oh well
                timetabler = null;
            }
            else {
//                System.out.println("Unknown/null message received");
                block();
            }
            //if all students shutdown and timetabler has been set to null shut self down
            if (studentAgents.size() < 1 && timetabler == null) {
                takeDown();
            }
        }
        
    }
    
    private void CalculateTotalStats() {
        if (studentUtilities != null) {
            totalSystemUtility = studentUtilities.values().stream().reduce((long) 0, Long::sum);
            averageSystemUtility = (float) totalSystemUtility / (float) numberOfStudents;
        }
        if (studentMessagesSent != null) {
            
            totalMessagesSent = studentMessagesSent.values().stream().reduce((long) 0, Long::sum);
            averageMessagesSent = (float) totalMessagesSent / (float) numberOfStudents;
        }
        
        utilityPolls.add(new String[]{
                String.valueOf(TimeUnit.MILLISECONDS.toSeconds(((System.currentTimeMillis())))),
                String.valueOf(totalSystemUtility),
                String.valueOf(averageSystemUtility),
                String.valueOf(totalMessagesSent), String.valueOf(averageMessagesSent)
        });
        
    }
    
    protected void takeDown()
    {
        
        System.out.println("Final total system utility = " + totalSystemUtility);
        System.out.println("Final average system utility = " + averageSystemUtility);
        System.out.println("Final total messages sent = " + totalMessagesSent);
        System.out.println("Final average messages sent = " + averageMessagesSent);
        
        if (lowAverageUtilityThresholdTimeReached != 0) {
            System.out.println("Utility lowAverageUtilityThresholdTimeReached reached in = " + (lowAverageUtilityThresholdTimeReached - timeSwapBehaviourStarted) / 1000 + " seconds");
            
        }
        if (mediumAverageUtilityThresholdTimeReached != 0) {
            
            System.out.println("Utility mediumAverageUtilityThresholdTimeReached reached in = " + (mediumAverageUtilityThresholdTimeReached - timeSwapBehaviourStarted) / 1000 + " seconds");
        }
        if (finalAverageUtilityThresholdTimeReached != 0) {
            System.out.println("Final utility threshold reached in = " + (finalAverageUtilityThresholdTimeReached - timeSwapBehaviourStarted) / 1000 + " seconds");
        }
        
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (
                FIPAException fe) {
            fe.printStackTrace();
        }
        
        System.out.println("Timetabler " + getAID().getName() + " terminating.");
    }
    
    public void writeStatsToCSVFile() throws IOException {
        List<String[]> dataLines = new ArrayList<>();
        
        var titleLine = new String[]{
                "runID",
                
                "totalSystemUtility",
                "averageSystemUtility",
                "totalMessagesSent",
                "averageMessagesSent",
                
                "initialTotalUtility",
                "initialAverageUtility",
                
                "netTotalUtilityChange",
                "netAverageUtilityChange",
                
                "bestStudentUtility",
                "worstStudentUtility",
                
                "lowAverageUtilityThreshold",
                "lowAverageUtilityThresholdTimeReached",
                "mediumAverageUtilityThreshold",
                "mediumAverageUtilityThresholdTimeReached",
                "finalAverageUtilityThreshold",
                "finalAverageUtilityThresholdTimeReached",
                
                "numberOfModules",
                "tutorialGroupsPerModule",
                "numberOfStudents",
                "modulesPerStudent",
                "highMinimumSwapUtilityGain",
                "mediumMinimumSwapUtilityGain",
                "lowMinimumSwapUtilityGain",
                "mediumUtilityThreshold",
                "highUtilityThreshold",
                "unwantedSlotCheckPeriod",
                "maxRunTimeSecs",
                "runTime"
        };
        
        var numberOfModules = runConfig.get(0);
        var tutorialGroupsPerModule = runConfig.get(1);
        
        var numberOfStudents = runConfig.get(2);
        
        var modulesPerStudent = runConfig.get(3);
        
        //student tuning
        var highMinimumSwapUtilityGain = runConfig.get(4);
        var mediumMinimumSwapUtilityGain = runConfig.get(5);
        var lowMinimumSwapUtilityGain = runConfig.get(6);
        var mediumUtilityThreshold = runConfig.get(7);
        var highUtilityThreshold = runConfig.get(8);
        var unwantedSlotCheckPeriod = runConfig.get(9);
        var runId = TimeUnit.MILLISECONDS.toSeconds(((System.currentTimeMillis())));
        
        dataLines.add(new String[]
                              {String.valueOf(runId),
                                      String.valueOf(totalSystemUtility),
                                      String.valueOf(averageSystemUtility),
                                      String.valueOf(totalMessagesSent),
                                      String.valueOf(averageMessagesSent),
                                      String.valueOf(initialTotalUtility),
                                      String.valueOf(initialTotalUtility / numberOfStudents),
                                      String.valueOf(totalSystemUtility - initialTotalUtility),
                                      String.valueOf((totalSystemUtility - initialTotalUtility) / numberOfStudents),
                                      String.valueOf(bestStudentUtility),
                                      String.valueOf(worstStudentUtility),
                                      String.valueOf(lowAverageUtilityThreshold),
                                      String.valueOf(lowAverageUtilityThresholdTimeReached),
                                      String.valueOf(mediumAverageUtilityThreshold),
                                      String.valueOf(mediumAverageUtilityThresholdTimeReached),
                                      String.valueOf(finalAverageUtilityThreshold),
                                      String.valueOf(finalAverageUtilityThresholdTimeReached),
                                      String.valueOf(numberOfModules),
                                      String.valueOf(tutorialGroupsPerModule),
                                      String.valueOf(numberOfStudents),
                                      String.valueOf(modulesPerStudent),
                                      String.valueOf(highMinimumSwapUtilityGain),
                                      String.valueOf(mediumMinimumSwapUtilityGain),
                                      String.valueOf(lowMinimumSwapUtilityGain),
                                      String.valueOf(mediumUtilityThreshold),
                                      String.valueOf(highUtilityThreshold),
                                      String.valueOf(unwantedSlotCheckPeriod),
                                      String.valueOf(maxRunTime * 1000),
                                      runTime
                    
                              });
        
        File finalRunCsvOutputFile = new File("~/finalRunData.csv");
        
        String finalRunCsvOutput = "";
        
        if (finalRunCsvOutputFile.length() == 0) {
            finalRunCsvOutput = String.join(",", titleLine) + "\n";
        }
        else {
            for (String[] dataLine : dataLines) {
                finalRunCsvOutput = finalRunCsvOutput + String.join(",", dataLine) + "\n";
                
            }
        }
        FileOutputStream fos = new FileOutputStream("~/finalRunData.csv", true);
        fos.write(finalRunCsvOutput.getBytes());
        fos.close();
        
        var runTitleLine = "systemTime,totalSystemUtility,averageSystemUtility,totalMessagesSent,averageMessagesSent" + "\n";
        var runFileName = "run-" + runId;
        var runOutput = runTitleLine;
        for (String[] poll : utilityPolls) {
            runOutput = runOutput + String.join(",", poll) + "\n";
        }
        
        FileOutputStream runfos = new FileOutputStream("~/" + runFileName + ".csv", true);
        runfos.write(finalRunCsvOutput.getBytes());
        runfos.close();
        
    }
    
}