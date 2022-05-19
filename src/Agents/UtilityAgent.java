package Agents;

import Ontology.Elements.AreCurrentFor;
import Ontology.Elements.StudentAgentMetrics;
import Ontology.MultiAgentTimetablerOntology;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UtilityAgent extends Agent
{
    private Codec codec = new SLCodec();
    private Ontology ontology = MultiAgentTimetablerOntology.getInstance();
    private ArrayList<AID> studentAgents;
    private AID timetabler;
    private int numberOfStudentsRegistered;
    
    //keeping in separate ones to allow for stream reduce
    private HashMap<AID, Long> studentTimetableUtilities;
    private HashMap<AID, Long> studentMessagesSent;
    private ArrayList<StudentAgentMetrics> studentAgentMetrics;
    
    private long maxRunTime;
    
    private long utilityPollPeriod;
    
    private long totalSystemTimetableUtility;
    
    private float averageSystemTimetableUtility;
    
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
        System.out.println("Utility waiting for student agents' registration...");
        addBehaviour(new RegistrationReceiver());
        
        utilityPolls = new ArrayList<>();
        bestStudentUtility = 0;
        worstStudentUtility = 100000;
        
        var numberOfModules = runConfig.get(0);
        
        var numberOfStudentsExpected = Math.toIntExact(runConfig.get(2));

//     utility tuning
        utilityPollPeriod = runConfig.get(10);
        
        lowAverageUtilityThreshold = runConfig.get(11);
        
        mediumAverageUtilityThreshold = runConfig.get(12);
        
        finalAverageUtilityThreshold = runConfig.get(13);
        
        var maxRunTimeSecs = runConfig.get(14);
        maxRunTime = maxRunTimeSecs * 1000;
        
        studentTimetableUtilities = new HashMap<>();
        studentMessagesSent = new HashMap<>();

//        System.out.println("Waiting for student agents' registration...");
//        addBehaviour(new RegistrationReceiver());
        
        addBehaviour(new WakerBehaviour(this, 10000)
        {
            @Override
            protected void onWake() {
                super.onWake();
                addBehaviour(new MetricsReceiver());
                
                addBehaviour(new MetricsPoller(this.myAgent, utilityPollPeriod));
                
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
            var msg = receive(mt);
            
            if (msg == null) {
                block();
            }
            else if (msg.getConversationId().equals("register-utility")) {
                if (msg.getContent() == null) {
                    studentAgents.add(msg.getSender());
                    
                    var reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.addReceiver(msg.getSender());
                    reply.setLanguage(codec.getName());
                    reply.setOntology(ontology.getName());
                    reply.setConversationId("register-utility");
                    reply.setContent("true");
                    send(reply);
                    
                    numberOfStudentsRegistered = studentAgents.size();
                    
                }
                else if (msg.getContent().equals("timetabler")) {
                    timetabler = msg.getSender();
                    
                }
                else {
//                System.out.println("Unknown/null message received");
                    block();
                    return;
                }
            }
            
        }
        
    }
    
    public class MetricsPoller extends TickerBehaviour
    {
        public MetricsPoller(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            addBehaviour(new MetricsPollBroadcaster());
            
            CalculateTotalMetrics();
            
        }
        
    }
    
    public class MetricsPollBroadcaster extends ParallelBehaviour
    {
        public MetricsPollBroadcaster() {
            super(ParallelBehaviour.WHEN_ALL);
        }
        
        @Override
        public void onStart() {
            studentAgents.forEach(studentAgent -> {
                                      addSubBehaviour(new MetricsPollRequester(studentAgent));
                                  }
            
            );
        }
    }
    
    public class MetricsPollRequester extends OneShotBehaviour
    {
        private AID studentAgent;
        
        public MetricsPollRequester(AID studentAgent) {
            
            this.studentAgent = studentAgent;
        }
        
        @Override
        public void action() {
            var msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(studentAgent);
            msg.setConversationId("current-metrics");
            msg.setLanguage(codec.getName());
            msg.setOntology(ontology.getName());
            msg.setContent("Please report your current Metrics");
            send(msg);
        }
    }
    
    public class MetricsReceiver extends CyclicBehaviour
    {
        
        @Override
        public void action() {
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("current-metrics"));
            var msg = receive(mt);
            
            if (msg == null) {
//                System.out.println("Unknown/null message received");
//                System.out.println("Sender:" + msg.getSender());
                
                block();
            }
            else {
                try {
                    ContentElement contentElement;

//                    System.out.println(msg.getContent()); //print out the message content in SL
                    
                    // Let JADE convert from String to Java objects
                    // Output will be a ContentElement
                    contentElement = getContentManager().extractContent(msg);
                    
                    if (contentElement instanceof AreCurrentFor) {
                        var areCurrentFor = (AreCurrentFor) contentElement;
                        var student = msg.getSender();
                        var Metrics = areCurrentFor.getStudentMetrics();
                        var utility = Metrics.getCurrentTimetableUtility();
                        var messagesSent = Metrics.getMessagesSent();
                        
                        studentTimetableUtilities.put(student, utility);
                        studentMessagesSent.put(student, messagesSent);
                        
                        if (Metrics.isInitialMetrics()) {
                            initialTotalUtility += utility;
                        }
                        //could add extra messages sent here but probably not entirely necessary
                        if (Metrics.isFinalMetrics()) {
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
                CalculateTotalMetrics();
            }
            
        }
    }
    
    public class ThresholdReached extends OneShotBehaviour
    {
        @Override
        public void action() {
            
            addBehaviour(new LowAverageUtilityThresholdTimeReached(this.myAgent, 2000));
            addBehaviour(new MediumAverageUtilityThresholdTimeReached(this.myAgent, 2000));
            addBehaviour(new FinalAverageUtilityThresholdReached(this.myAgent, 5000));
            addBehaviour(new MaxRunTimeReached(this.myAgent, maxRunTime));
            
        }
    }
    
    public class LowAverageUtilityThresholdTimeReached extends TickerBehaviour
    {
        public LowAverageUtilityThresholdTimeReached(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            if (averageSystemTimetableUtility >= lowAverageUtilityThreshold) {
                lowAverageUtilityThresholdTimeReached = System.currentTimeMillis();
            }
        }
    }
    
    public class MediumAverageUtilityThresholdTimeReached extends TickerBehaviour
    {
        public MediumAverageUtilityThresholdTimeReached(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            if (averageSystemTimetableUtility >= mediumAverageUtilityThreshold) {
                mediumAverageUtilityThresholdTimeReached = System.currentTimeMillis();
            }
        }
    }
    
    public class FinalAverageUtilityThresholdReached extends TickerBehaviour
    {
        public FinalAverageUtilityThresholdReached(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            if (averageSystemTimetableUtility >= finalAverageUtilityThreshold) {
                finalAverageUtilityThresholdTimeReached = System.currentTimeMillis();
                
                System.out.println("Final average utility threshold at " + averageSystemTimetableUtility + " reached, notifying shutdown.");
                
                addBehaviour(new NotifyEnd());
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
            
            addBehaviour(new NotifyEnd());
            
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
//                var msg = receive(mt);
//
//                if (msg != null && msg.getSender() == studentAgent) {
//                    var studentUtility = Float.parseFloat(msg.getContent());
//                    studentTimetableUtilities.put(studentAgent, studentUtility);
//                }
//
            });
            
            endMsg.addReceiver(timetabler);
            send(endMsg);
            
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("end"));
            var msg = receive(mt);
            
            if (msg != null && msg.getSender() == timetabler) {
                timeSwapBehaviourStarted = Long.parseLong(msg.getContent());

//                System.out.println("Swap Behaviour ran for: " + timeSwapBehaviourEnded + " seconds");
            
            }
            else {
                block();
            }
            CalculateTotalMetrics();
            
            //would be better to print this to a file
            System.out.println("Total system Utility: " + totalSystemTimetableUtility);
            System.out.println("Average system Utility: " + averageSystemTimetableUtility);
            
            System.out.println("Initial system Utility: " + initialTotalUtility);
            var initAvgUtil = (float) initialTotalUtility / (float) numberOfStudentsRegistered;
            System.out.println("Initial average system Utility: " + initAvgUtil);
            
            System.out.println("Net Total system Utility change: " + (totalSystemTimetableUtility - initialTotalUtility));
            System.out.println("Net Average system Utility change: " + (averageSystemTimetableUtility - initAvgUtil));
            
            System.out.println("Total messages sent: " + totalMessagesSent);
            System.out.println("Average messages sent: " + averageMessagesSent);
            
        }
    }
    
    private class AgentShutdownListener extends CyclicBehaviour
    {
        
        @Override
        public void action() {
            var mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("ending"));
            var msg = receive(mt);
            
            if (msg == null) {
                block();
            }
            else {
                if (studentAgents.contains(msg.getSender())) {
                    
                    studentAgents.remove(msg.getSender());
                    
                    System.out.println("Student " + msg.getSender() + " shutting down and removed from utility agent.");
                    
                }
                else if (msg != null && msg.getSender() == timetabler) {
                    runTime = msg.getContent();
                    //crude but oh well
                    timetabler = null;
                }
                
                //if all students shutdown and timetabler has been set to null shut self down
                if (studentAgents.size() < 1 && timetabler == null) {
                    takeDown();
                }
            }
            
        }
    }
    
    private void CalculateTotalMetrics() {
        if (studentTimetableUtilities != null) {
            totalSystemTimetableUtility = studentTimetableUtilities.values().stream().reduce((long) 0, Long::sum);
            averageSystemTimetableUtility = (float) totalSystemTimetableUtility / (float) numberOfStudentsRegistered;
        }
        if (studentMessagesSent != null) {
            
            totalMessagesSent = studentMessagesSent.values().stream().reduce((long) 0, Long::sum);
            averageMessagesSent = (float) totalMessagesSent / (float) numberOfStudentsRegistered;
        }
        
        utilityPolls.add(new String[]{
                String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())),
                String.valueOf(totalSystemTimetableUtility),
                String.valueOf(averageSystemTimetableUtility),
                String.valueOf(totalMessagesSent), String.valueOf(averageMessagesSent)
        });
        
    }
    
    private void CalculateFinalMetrics() {
        if (studentTimetableUtilities != null) {
            totalSystemTimetableUtility = studentTimetableUtilities.values().stream().reduce((long) 0, Long::sum);
            averageSystemTimetableUtility = (float) totalSystemTimetableUtility / (float) numberOfStudentsRegistered;
        }
        if (studentMessagesSent != null) {
            
            totalMessagesSent = studentMessagesSent.values().stream().reduce((long) 0, Long::sum);
            averageMessagesSent = (float) totalMessagesSent / (float) numberOfStudentsRegistered;
        }
        
        utilityPolls.add(new String[]{
                String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())),
                String.valueOf(totalSystemTimetableUtility),
                String.valueOf(averageSystemTimetableUtility),
                String.valueOf(totalMessagesSent), String.valueOf(averageMessagesSent)
        });
        
    }
    
    protected void takeDown()
    {
        CalculateFinalMetrics();
        
        System.out.println("Final total system utility = " + totalSystemTimetableUtility);
        System.out.println("Final average system utility = " + averageSystemTimetableUtility);
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
        
        try {
            writeSystemMetricsToCSVFile();
        }
        catch (IOException e) {
            e.printStackTrace();
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
    
    public String writeAgentMetricsToCSV() {
//        var titleLine = printMetricsFieldsToCsv();
        var csvOutput = printMetricsFieldsToCsv();
        
        for (StudentAgentMetrics studentMetrics : studentAgentMetrics) {
            csvOutput += printMetricsValsToCsv(studentMetrics);
        }
        return csvOutput;
    }
    
    public static String printMetricsFieldsToCsv() {
        String str = "AID,";
        
        for (Field field : StudentAgentMetrics.class.getDeclaredFields()) {
            
            str += field.getName() + ",";
            
        }
        str += '\n';
        return str;
    }
    
    public static String printMetricsValsToCsv(StudentAgentMetrics studentAgentMetrics) {
        String str = "AID,";
        
        Class<?> c = studentAgentMetrics.getClass();
        Field[] fields = c.getDeclaredFields();
        Map<String, Object> temp = new HashMap<String, Object>();
        
        for (Field field : fields) {
            try {
                str += field.get(studentAgentMetrics).toString();
//                temp.put(field.getName().toString(), field.get(studentAgentMetrics));
            }
            catch (IllegalArgumentException e1) {
            }
            catch (IllegalAccessException e1) {
            }
        }
        str += '\n';
        return str;
    }
    
    public List<String[]> writeSystemMetricsToDatalines(long runId) {
        List<String[]> dataLines = new ArrayList<>();
        
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
        
        dataLines.add(new String[]
                              {String.valueOf(runId),
                                      String.valueOf(totalSystemTimetableUtility),
                                      String.valueOf(averageSystemTimetableUtility),
                                      String.valueOf(totalMessagesSent),
                                      String.valueOf(averageMessagesSent),
                                      String.valueOf(initialTotalUtility),
                                      String.valueOf(initialTotalUtility / numberOfStudents),
                                      String.valueOf(totalSystemTimetableUtility - initialTotalUtility),
                                      String.valueOf((totalSystemTimetableUtility - initialTotalUtility) / numberOfStudents),
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
        return dataLines;
    }
    
    public List<String[]> writeSystemMetricsFieldsToDataLine() {
        List<String[]> dataLines = new ArrayList<>();
        
        var titleLine = new String[]{
                "runID",
                
                "totalSystemTimetableUtility",
                "averageSystemTimetableUtility",
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
        dataLines.add(titleLine);
        return dataLines;
    }
    
    public void writeSystemMetricsToCSVFile() throws IOException {
        var runId = TimeUnit.MILLISECONDS.toSeconds(((System.currentTimeMillis())));
        var title = writeSystemMetricsFieldsToDataLine();
        var metrics = writeSystemMetricsToDatalines(runId);
        
        String finalRunCsvOutput = "";
        
        var currentDirectory = System.getProperty("user. dir");
        
        Files.createDirectories(Paths.get(currentDirectory + "/runResults"));
        
        File finalRunCsvOutputFile = new File(currentDirectory + "/runResults/finalRunData.csv");
        
        if (finalRunCsvOutputFile.length() == 0) {
            finalRunCsvOutput = String.join(",", title.get(0)) + "\n";
        }
        else {
            for (String[] dataLine : metrics) {
                finalRunCsvOutput = finalRunCsvOutput + String.join(",", dataLine) + "\n";
                
            }
        }
        
        FileOutputStream fos = new FileOutputStream(currentDirectory + "/runResults/finalRunData.csv", true);
        fos.write(finalRunCsvOutput.getBytes());
        fos.close();
        
        var agentMetricsOutput = writeAgentMetricsToCSV();
        
        var runFileName = "run-" + runId;
        
        FileOutputStream runfos = new FileOutputStream(currentDirectory + "/runResults/" + runFileName + ".csv", true);
        runfos.write(agentMetricsOutput.getBytes());
        runfos.close();
        
    }
    
}
