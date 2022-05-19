import Agents.StudentAgent;
import Agents.TimetablerAgent;
import Agents.UtilityAgent;
import Generation.ModuleGeneration;
import Generation.TimetableGeneration;
import Models.Module;
import Models.Student;
import Models.Tutorial;
import Objects.Preference;
import Objects.StudentTimetablePreferences;
import Ontology.Elements.TutorialSlot;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;

import java.util.ArrayList;
import java.util.HashMap;
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
    private static float finalAverageUtilityThreshold;
    
    private static long maxRunTimeSecs;
    
    //student agent tuning
    private static int highMinimumSwapUtilityGain;
    private static int mediumMinimumSwapUtilityGain;
    private static int lowMinimumSwapUtilityGain;
    
    private static int mediumAgentUtilityThreshold;
    private static int highAgentUtilityThreshold;
    
    private static long unwantedSlotCheckPeriod;
    
    private static ArrayList<Long> runConfig;
    private static HashMap<String, Long> runConfigHashMap;
    
    private static boolean unhappyMode;
    
    public static void main(String[] args)
    {
        //setup jade environment
        Profile myProfile = new ProfileImpl();
        Runtime myRuntime = Runtime.instance();
        
        initiateConfig();
        
        
        generateTimetableAndStudents();
        
        var myContainer = myRuntime.createMainContainer(myProfile);
        
        var startedAgentMatrics = new ArrayList<Long>();
        
        try {
            
            //start agent controller (also an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();
            
            AgentController utilityAgent = myContainer.createNewAgent("utilityAgent", UtilityAgent.class.getCanonicalName(), new Object[]{runConfig});
            utilityAgent.start();
            
            AgentController timetablerAgent = myContainer.createNewAgent("timetabler", TimetablerAgent.class.getCanonicalName(), new Object[]{
                    modules,
                    students,
            });
            timetablerAgent.start();
            
            for (int i = 0; i < students.size(); i++) {
                
                var student = students.get(i);
                
                if (startedAgentMatrics.contains(student.getMatriculationNumber())) {
                    var studentwtf = student;
                }
                
                var matriculationNumber = Long.toString(student.getMatriculationNumber());
                
                var attendingModules = new ArrayList<Module>();
                
                modules.forEach(module -> {
                    if (student.getModuleIds().contains(module.getModuleId())) {
                        attendingModules.add(module);
                    }
                });
                
                AgentController studentAgent = myContainer.createNewAgent(matriculationNumber,
                                                                          StudentAgent.class.getCanonicalName(),
                                                                          new Object[]{student,
                                                                                  highMinimumSwapUtilityGain,
                                                                                  mediumMinimumSwapUtilityGain,
                                                                                  lowMinimumSwapUtilityGain,
                                                                                  mediumAgentUtilityThreshold,
                                                                                  highAgentUtilityThreshold,
                                                                                  unwantedSlotCheckPeriod,
                                                                                  attendingModules});
                
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
    
    private static void initiateConfig(){
                initTestCase0();
//        initTestCase1();
//            initTestCase2();
//            initTestCase3();
        defaultUtilityConfig();
    
        runConfig = new ArrayList<Long>();
        runConfig.add((long) numberOfModules);
        runConfig.add((long) tutorialGroupsPerModule);
        runConfig.add((long) numberOfStudents);
        runConfig.add((long) modulesPerStudent);
        runConfig.add((long) highMinimumSwapUtilityGain);
        runConfig.add((long) mediumMinimumSwapUtilityGain);
        runConfig.add((long) lowMinimumSwapUtilityGain);
        runConfig.add((long) mediumAgentUtilityThreshold);
        runConfig.add((long) highAgentUtilityThreshold);
        runConfig.add(unwantedSlotCheckPeriod);
        runConfig.add(utilityPollPeriod);
        runConfig.add((long) lowAverageUtilityThreshold);
        runConfig.add((long) mediumAverageUtilityThreshold);
        runConfig.add((long) finalAverageUtilityThreshold);
        runConfig.add(maxRunTimeSecs);
    
        //modules & students randomly generated here, would in reality be known & input via e.g. csv
        //i do believe this used to be more neatly refactored out but for reasons now lost to the sands of time it was easier to move them here
        //also it is a whole mess but sure look
        students = new ArrayList<>();
        modules = new ArrayList<>();
        tutorialSlots = new ArrayList<>();
        existingMatrics = new ArrayList<>();
    }
    
    private static void generateTimetableAndStudents() {
        modules = ModuleGeneration.initialiseModules(numberOfModules, tutorialGroupsPerModule, numberOfStudents);
        
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
            if(unhappyMode){
    
                student.setStudentTimetablePreferences(generateUnhappyStudentPreferences(student.getTutorialSlots()));
            }
            students.add(student);
            
        }
        
    }
    
    private static void generateMostBasicModulesAndConfig() {

//            actually i will hand-do this
        //generation tuning
//        numberOfModules = 1;
//        tutorialGroupsPerModule = 2;
//        numberOfStudents = 2;
//        modulesPerStudent = 1;
        var moduleId = "SET69420";
        Module module = new Module();
        module.setModuleId(moduleId);
        module.setTutorialGroupAmount(2);
        
        var moduleIds = new ArrayList<String>();
        moduleIds.add(moduleId);
        
        Tutorial tutorial1 = new Tutorial();
        Tutorial tutorial2 = new Tutorial();
        
        tutorial1.setCapacity(1L);
        tutorial2.setCapacity(1L);
        
        tutorial1.setModuleId(moduleId);
        tutorial2.setModuleId(moduleId);
        
        tutorial1.setTimeSlotId(11);
        tutorial2.setTimeSlotId(17);
        
        TutorialSlot tutorialSlot1 = new TutorialSlot(moduleId, 11);
        TutorialSlot tutorialSlot2 = new TutorialSlot(moduleId, 17);
        
        ArrayList<TutorialSlot> tutorialSlots = new ArrayList<>();
        tutorialSlots.add(tutorialSlot1);
        tutorialSlots.add(tutorialSlot2);
        
        ArrayList<TutorialSlot> tutorialSlots1 = new ArrayList<>();
        ArrayList<TutorialSlot> tutorialSlots2 = new ArrayList<>();
        tutorialSlots1.add(tutorialSlot1);
        tutorialSlots2.add(tutorialSlot2);

//
        Student student1 = new Student();
        student1.setMatriculationNumber(1L);
        student1.setModuleIds(moduleIds);
        student1.setTutorialSlots(tutorialSlots1);
        tutorial1.addStudent(student1);
        module.addEnrolledStudentId(student1.getMatriculationNumber());
        StudentTimetablePreferences s1prefs = new StudentTimetablePreferences();
        s1prefs.setPreference(11, Preference.PREFER_NOT);
        student1.setStudentTimetablePreferences(s1prefs);
        
        Student student2 = new Student();
        student2.setMatriculationNumber(2L);
        student2.setModuleIds(moduleIds);
        student2.setTutorialSlots(tutorialSlots2);
        tutorial2.addStudent(student2);
        module.addEnrolledStudentId(student1.getMatriculationNumber());
        student1.setStudentTimetablePreferences(new StudentTimetablePreferences());
        
        ArrayList<Tutorial> tutorials = new ArrayList<>();
        tutorials.add(tutorial1);
        tutorials.add(tutorial2);
        module.setTutorials(tutorials);
        
    }
    
    private static void defaultUtilityConfig() {
        unhappyMode=true;
        
        //student tuning
        highMinimumSwapUtilityGain = Preference.PREFER.getUtility() - Preference.NO_PREFERENCE.getUtility();
        mediumMinimumSwapUtilityGain = Preference.CANNOT.getUtility() - Preference.NO_PREFERENCE.getUtility();
        lowMinimumSwapUtilityGain = Preference.PREFER_NOT.getUtility() - Preference.CANNOT.getUtility();
        
        mediumAgentUtilityThreshold = numberOfModules * Preference.NO_PREFERENCE.getUtility();
        highAgentUtilityThreshold = numberOfModules * Preference.PREFER.getUtility();
        
        unwantedSlotCheckPeriod = (long) 5000;
        
        //utility tuning
        utilityPollPeriod = (long) 10000;
        lowAverageUtilityThreshold = numberOfModules * Preference.PREFER_NOT.getUtility();
        mediumAverageUtilityThreshold = numberOfModules * Preference.NO_PREFERENCE.getUtility();
        finalAverageUtilityThreshold = numberOfModules * Preference.PREFER.getUtility();//all preferences satisfied
        
        var maxRunTimeMins = 10;
        maxRunTimeSecs = maxRunTimeMins * 60;
    }
    
    private static void initTestCase00() {
        //generation tuning
        numberOfModules = 1;
        tutorialGroupsPerModule = 2;
        numberOfStudents = 2;
        modulesPerStudent = 1;
        
    }
    
    
    
    private static void initTestCase0() {
        //generation tuning
        numberOfModules = 1;
        tutorialGroupsPerModule = 5;
        numberOfStudents = 10;
        modulesPerStudent = 1;
        
    }
    
    private static void initTestCase1() {
        //generation tuning
        numberOfModules = 3;
        tutorialGroupsPerModule = 3;
        numberOfStudents = 50;
        modulesPerStudent = 3;
        
    }
    
    private static void initTestCase2() {
        
        var numberOfModules = 3;
        var tutorialGroupsPerModule = 3;
        numberOfStudents = 200;
        modulesPerStudent = 3;
    }
    
    private static void initTestCase3() {
        
        var numberOfModules = 3;
        //random amount of tutorial groups per module
        var tutorialGroupsPerModule = 10;
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
    
    public static StudentTimetablePreferences generateUnhappyStudentPreferences(ArrayList<TutorialSlot> assignedTutorials) {
            var studentPreferences = new StudentTimetablePreferences();
    
        modules.forEach(module -> {
            module.getTutorials().forEach(tutorial -> {
                studentPreferences.setPreference(tutorial.getTimeslotId(), Preference.PREFER);
    
            });
        });
        
        assignedTutorials.forEach(tutorialSlot -> {
            studentPreferences.setPreference(tutorialSlot.getTimeslotId(), Preference.PREFER_NOT);
        });
      
        return studentPreferences;
    }
    
}
