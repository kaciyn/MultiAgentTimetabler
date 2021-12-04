import Agents.StudentAgent;
import Agents.TimetablerAgent;
import Ontology.Elements.Student;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.HashSet;

public class Application
{
    private static int numberOfModules;
    private static int tutorialGroupsPerModule;
    private static int numberOfStudents;
    private static int modulesPerStudent;
    
    public static void main(String[] args)
    {
        //setup jade environment
        Profile myProfile = new ProfileImpl();
        Runtime myRuntime = Runtime.instance();
        
        //modules & students randomly generated here, would in reality be known & input via e.g. csv
        var modules = ModuleGeneration.initialiseModules(numberOfModules, tutorialGroupsPerModule, numberOfStudents);
        
        var timetable = TimetableGeneration.generateRandomTimetable(modules);
        
        var students = new HashSet<Student>();
        for (int i = 0; i < numberOfStudents; i++) {
            var student = StudentGeneration.generateRandomStudent();
            StudentGeneration.randomlyAssignModulesToStudent(student, modules, modulesPerStudent);
            students.add(student);
        }
        
        initTestCase0();
//            initTestCase1();
//            initTestCase2();
//            initTestCase3();
        
        try {
            ContainerController myContainer = myRuntime.createMainContainer(myProfile);
            
            //start agent controller (also an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();
            
            AgentController timetablerAgent = myContainer.createNewAgent("timetabler", TimetablerAgent.class.getCanonicalName(), new Object[]{timetable,students});
            timetablerAgent.start();
    
        
            
            students.forEach(student -> {
                AgentController studentAgent = null;
                try {
                    //student aid is just the matric
                    studentAgent = myContainer.createNewAgent(Integer.toString(student.getMatriculationNumber()), StudentAgent.class.getCanonicalName(), null);
                }
                catch (StaleProxyException e) {
                    e.printStackTrace();
                }
                try {
                    studentAgent.start();
                }
                catch (StaleProxyException e) {
                    e.printStackTrace();
                }
            });
        }
        catch (Exception e) {
            System.out.println("Exception starting agent: " + e.toString());
            e.printStackTrace();
            
        }
        
    }
    
    private static void initTestCase0() {
        
        numberOfModules = 1;
        tutorialGroupsPerModule = 2;
        numberOfStudents = 100;
        modulesPerStudent = 1;
    }
    
    private static void initTestCase1() {
        
        numberOfModules = 2;
        tutorialGroupsPerModule = 2;
        numberOfStudents = 200;
        modulesPerStudent = 2;
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
        var tutorialGroupsPerModule = -1;
        numberOfStudents = 200;
        modulesPerStudent = 3;
    }
}
