import Agents.StudentAgent;
import Agents.TimetablerAgent;
import Agents.UtilityAgent;
import Generation.ModuleGeneration;
import Generation.TimetableGeneration;
import Models.Module;
import Models.Student;
import Objects.Preference;
import Objects.StudentTimetablePreferences;
import Ontology.Elements.TutorialSlot;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Application
{
    //module settings
    private static int numberOfModules;
    private static int tutorialGroupsPerModule;
    private static int numberOfStudents;
    private static int modulesPerStudent;
    
    private static ArrayList<Module> modules;
    private static ArrayList<TutorialSlot> tutorialSlots;
    private static ArrayList<Student> students;
    
    private static ArrayList<Long> existingMatrics;
    
    //utility tuning
    private static long utilityPollPeriod;
    
    private static float lowAverageUtilityThreshold;
    private static float mediumAverageUtilityThreshold;
    private static float finalAverageUtilityThreshold = (float) 3;
    
    private static long maxRunTimeSecs;
    
    //student agent tuning
    private static int highMinimumSwapUtilityGain;
    private static int mediumMinimumSwapUtilityGain;
    private static int lowMinimumSwapUtilityGain;
    
    private static int mediumUtilityThreshold;
    private static int highUtilityThreshold;
    
    private static long unwantedSlotCheckPeriod;
    
    private static ArrayList<Long> runConfig;
    
    private static long noSwapTimeThreshold;
    
    
    public static void main(String[] args)
    {
        //setup jade environment
        Profile myProfile = new ProfileImpl();
        Runtime myRuntime = Runtime.instance();
        
        runConfig=new ArrayList<Long>();

//        initTestCase0();
//        initTestCase1();
            initTestCase2();
//            initTestCase3();
        
        runConfig.add((long) numberOfModules);
        runConfig.add((long) tutorialGroupsPerModule);
        runConfig.add((long) numberOfStudents);
        runConfig.add((long) modulesPerStudent);
        runConfig.add((long) highMinimumSwapUtilityGain);
        runConfig.add((long) mediumMinimumSwapUtilityGain);
        runConfig.add((long) lowMinimumSwapUtilityGain);
        runConfig.add((long) mediumUtilityThreshold);
        runConfig.add((long) highUtilityThreshold);
        runConfig.add(unwantedSlotCheckPeriod);
        runConfig.add(utilityPollPeriod);
        runConfig.add((long) lowAverageUtilityThreshold);
        runConfig.add((long) mediumAverageUtilityThreshold);
        runConfig.add((long) finalAverageUtilityThreshold);
        runConfig.add(maxRunTimeSecs);
        runConfig.add(noSwapTimeThreshold);

        
        //modules & students randomly generated here, would in reality be known & input via e.g. csv
        //i do believe this used to be more neatly refactored out but for reasons now lost to the sands of time it was easier to move them here
        //also it is a whole mess but sure look
        students = new ArrayList<>();
        modules = new ArrayList<>();
        modules = ModuleGeneration.initialiseModules(numberOfModules, tutorialGroupsPerModule, numberOfStudents);
        tutorialSlots = new ArrayList<>();
        existingMatrics = new ArrayList<>();
        
        var timetable = TimetableGeneration.generateRandomTimetable(modules);
        
        for (int i = 0; i < numberOfStudents; i++) {
            var student = generateRandomStudent();
            
            if (modulesPerStudent > modules.size()) {
                modulesPerStudent = modules.size();
            }
            var r = new Random();
            var studentModuleIds = new ArrayList<String>();
            
            //randomly assign modules to students
            for (int j = 0; j < modulesPerStudent; j++) {
                int moduleIndex = r.nextInt(modules.size());
                //add random module to student
                studentModuleIds.add(modules.get(moduleIndex).getModuleId());
                //add student to module
                modules.get(moduleIndex).addEnrolledStudentId(student.getMatriculationNumber());
                
                //assign student to random module tutorial
//                for (int k = 0; k < modules.get(m).getTutorialGroupAmount(); k++) {
                var tutorialIndexIndex = new ArrayList<Integer>();
                for (int k = 0; k < modules.get(moduleIndex).getTutorialGroupAmount(); k++) {
                    tutorialIndexIndex.add(k);
                }
                
                int tutorialIndex = tutorialIndexIndex.get(r.nextInt(tutorialIndexIndex.size()));

//if tutorial is at capacity, remove from pool and pick another one
                var enough = false;
                while (modules.get(moduleIndex).getTutorials().get(tutorialIndex).getStudentIds().size() >= (long) (modules.get(moduleIndex).getTutorials().get(tutorialIndex).getCapacity()) && enough) {
                    
                    if (tutorialIndexIndex.size() <= 1) {
                        tutorialIndex = tutorialIndexIndex.get(0);
                        enough = true;
                    }
                    else {
                        tutorialIndexIndex.remove(tutorialIndex);
                        
                        tutorialIndex = tutorialIndexIndex.get(r.nextInt(tutorialIndexIndex.size()));
                    }
                }
                
                var currentModule = modules.get(moduleIndex);
                var currentModuleTutorials = currentModule.getTutorials();
                var selectedTutorial = currentModuleTutorials.get(tutorialIndex);
                var selectedTutorialTimeslotId = selectedTutorial.getTimeslotId();
                
                var tutorialSlot = new TutorialSlot(currentModule.getModuleId(), selectedTutorialTimeslotId);
                
                tutorialSlots.add(tutorialSlot);
                
                //add tutorialSlot to student
                student.addTutorialSlot(tutorialSlot);
                
                //add student to tutorial in module
                modules.get(moduleIndex).getTutorials().get(tutorialIndex).addStudent(student);
                
                student.setModuleIds(studentModuleIds);
                
            }
            students.add(student);
            
        }
        
        var myContainer = myRuntime.createMainContainer(myProfile);
        
        var startedAgentMatrics = new ArrayList<Long>();
        
        try {
            
            //start agent controller (also an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();
            
            
            AgentController timetablerAgent = myContainer.createNewAgent("timetabler", TimetablerAgent.class.getCanonicalName(), new Object[]{
                    modules,
                    students,
            });
            timetablerAgent.start();
    
            AgentController utilityAgent = myContainer.createNewAgent("utilityAgent", UtilityAgent.class.getCanonicalName(), new Object[]{runConfig});
            utilityAgent.start();
            
            for (int i = 0; i < students.size(); i++) {
                
                var student = students.get(i);
                
                if (startedAgentMatrics.contains(student.getMatriculationNumber())) {
                    var studentwtf = student;
                }
                
                var matriculationNumber = Long.toString(student.getMatriculationNumber());
                
                AgentController studentAgent = myContainer.createNewAgent(matriculationNumber,
                                                                          StudentAgent.class.getCanonicalName(),
                                                                          new Object[]{student,
                                                                                  highMinimumSwapUtilityGain,
                                                                                  mediumMinimumSwapUtilityGain,
                                                                                  lowMinimumSwapUtilityGain,
                                                                                  mediumUtilityThreshold,
                                                                                  highUtilityThreshold, unwantedSlotCheckPeriod,noSwapTimeThreshold});
                
                studentAgent.start();
                startedAgentMatrics.add(student.getMatriculationNumber());
                
            }
        }
        catch (
                Exception e) {
            System.out.println("Exception starting agent: " + e.toString());
            e.printStackTrace();
            
        }
        
    }
    
    private static void initTestCase0() {
        //generation tuning
        numberOfModules = 1;
        tutorialGroupsPerModule = 2;
        numberOfStudents = 50;
        modulesPerStudent = 1;
        
        //student tuning
        highMinimumSwapUtilityGain = 1;
        mediumMinimumSwapUtilityGain = 0;
        lowMinimumSwapUtilityGain = -1;
        
        mediumUtilityThreshold = -numberOfModules;
        highUtilityThreshold = numberOfModules * Preference.PREFER.getUtility();
        
        unwantedSlotCheckPeriod = (long) 1000;
        ;
        
        //utility tuning
        utilityPollPeriod = (long) 10000;
        lowAverageUtilityThreshold = -numberOfModules;
        mediumAverageUtilityThreshold = 0;
        finalAverageUtilityThreshold = (float) numberOfModules * 3;
        
        maxRunTimeSecs = (long) 1;
        
    }
    
    private static void initTestCase1() {
        numberOfModules = 2;
        tutorialGroupsPerModule = 2;
        numberOfStudents = 50;
        modulesPerStudent = 2;
        
        //student tuning
        highMinimumSwapUtilityGain = 1;
        mediumMinimumSwapUtilityGain = 0;
        lowMinimumSwapUtilityGain = -1;
        mediumUtilityThreshold = -numberOfModules;
        highUtilityThreshold = numberOfModules * Preference.PREFER.getUtility();
        unwantedSlotCheckPeriod = 2000;
        
        //utility tuning
        utilityPollPeriod = 5000;
        lowAverageUtilityThreshold = -numberOfModules;
        mediumAverageUtilityThreshold = 0;
        finalAverageUtilityThreshold = (float) numberOfModules * 3;
        var maxRunTimeMins = 10;
        maxRunTimeSecs = maxRunTimeMins * 60;
        
        noSwapTimeThreshold=2*60*1000;
        
    }
    
    private static void initTestCase2() {
        //normal amount of classes
        numberOfModules = 3;
        tutorialGroupsPerModule = 3;
        numberOfStudents = 150;
        modulesPerStudent = 3;
    
        //student tuning
        highMinimumSwapUtilityGain = 1;
        mediumMinimumSwapUtilityGain = 0;
        lowMinimumSwapUtilityGain = -1;
        mediumUtilityThreshold = -numberOfModules;
        highUtilityThreshold = numberOfModules * Preference.PREFER.getUtility();
        unwantedSlotCheckPeriod = 2000;
    
        //utility tuning
        utilityPollPeriod = 10000;
        lowAverageUtilityThreshold = -numberOfModules;
        mediumAverageUtilityThreshold = 0;
        finalAverageUtilityThreshold = (float) numberOfModules * 3;
        var maxRunTimeMins = 10;
        maxRunTimeSecs = maxRunTimeMins * 60;
    
        noSwapTimeThreshold=2*60*1000;
    }
    
    private static void initTestCase3() {
        utilityPollPeriod = 1000;
        
        lowAverageUtilityThreshold = -100;
        mediumAverageUtilityThreshold = 0;
        finalAverageUtilityThreshold = 3;
        
        var numberOfModules = 3;
        //random amount of tutorial groups per module
        var tutorialGroupsPerModule = -1;
        numberOfStudents = 200;
        modulesPerStudent = 3;
    }
    
    private static Student generateRandomStudent() {
        var student = new Student();
        student.setMatriculationNumber(generateRandomStudentMatriculationNumber());
        student.setStudentTimetablePreferences(generateRandomStudentPreferences());
        return student;
    }
    
    private static long generateRandomStudentMatriculationNumber() {
        var newMatriculationNumber = 4000000 + (long) (ThreadLocalRandom.current().nextFloat() * 900000);
        
        while (existingMatrics.contains(newMatriculationNumber)) {
            newMatriculationNumber = 4000000 + (long) (ThreadLocalRandom.current().nextFloat() * 900000);
        }
        existingMatrics.add(newMatriculationNumber);
        return newMatriculationNumber;
    }
    
    public static StudentTimetablePreferences generateRandomStudentPreferences() {
        var studentPreferences = new StudentTimetablePreferences();
        for (int i = 1; i < 45; i++) {
            var preference = Helpers.getLinearRandomNumber(4);
            
            //linear random distribution, from most likely to least: no pref,prefer, prefer not, cannot
            //this is fairly arbitrary but didn't think a student would/should be equally likely to not be able attend slots at the same probability as everything
            switch (preference) {
                case 1:
                    studentPreferences.setPreference(i, Preference.NO_PREFERENCE);
                    break;
                case 2:
                    studentPreferences.setPreference(i, Preference.PREFER);
                    break;
                case 3:
                    studentPreferences.setPreference(i, Preference.PREFER_NOT);
                    break;
                case 4:
                    studentPreferences.setPreference(i, Preference.CANNOT);
                    break;
            }
        }
        
        return studentPreferences;
    }
    
}
