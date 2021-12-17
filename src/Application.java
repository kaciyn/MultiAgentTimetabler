import Agents.StudentAgent;
import Agents.TimetablerAgent;
import Ontology.Elements.Module;
import Ontology.Elements.Student;
import Ontology.Elements.Timetable;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.util.ArrayList;
import java.util.Random;

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
        
        initTestCase0();
//        initTestCase1();
//            initTestCase2();
//            initTestCase3();
        
        //modules & students randomly generated here, would in reality be known & input via e.g. csv
        
        var modules = ModuleGeneration.initialiseModules(numberOfModules, tutorialGroupsPerModule, numberOfStudents);
        
        var timetable = TimetableGeneration.generateRandomTimetable(modules);
        
        var students = new ArrayList<Student>();
        
        for (int i = 0; i < numberOfStudents; i++) {
            var student = StudentGeneration.generateRandomStudent();
            
            if (modulesPerStudent > modules.size()) {
                modulesPerStudent = modules.size();
            }
            var r = new Random();
            var studentModuleIds = new ArrayList<String>();
            
            for (int j = 0; j < modulesPerStudent; j++) {
                int m = r.nextInt(modules.size());
                studentModuleIds.add(modules.get(m).getModuleId());
                modules.get(m).addEnrolledStudentId(student.getMatriculationNumber());
            }
            
            student.setModuleIds(studentModuleIds);
            
            students.add(student);
        }
        
        for (int i = 0; i < modules.size(); i++) {
            var r = new Random();
            
            modules.get(i).getTutorials().forEach(tutorial -> {
        
                var studentsInTutorial = new ArrayList<Student>();
        
                //keep adding random students on module until tutorial is full
                while (studentsInTutorial.size() <= tutorial.getCapacity()) {
                    var randomStudent = studentsInModule.get(r.nextInt(studentsInModule.size()));
                    studentsInTutorial.add(randomStudent);
                    randomStudent.addTutorial(tutorial);
            
                    assignedStudentsInModule.add(randomStudent);
                }
        
            });
            tutorialedStudents.addAll(ModuleGeneration.randomlyAssignStudentsToTutorials(modules.get(i), ));
            
        }
        
        var tutorialedStudents = new ArrayList<Student>();
        
        ContainerController myContainer = myRuntime.createMainContainer(myProfile);
        
        try {
            
            //start agent controller (also an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();
            
            AgentController timetablerAgent = myContainer.createNewAgent("timetabler", TimetablerAgent.class.getCanonicalName(), new Object[]{timetable, moduledStudents});
            timetablerAgent.start();
            
            for (int i = 0; i < moduledStudents.size(); i++) {
                var student = moduledStudents.get(i);
                var matriculationNumber = Integer.toString(student.getMatriculationNumber());
                
                AgentController studentAgent = myContainer.createNewAgent(matriculationNumber, StudentAgent.class.getCanonicalName(), new Object[]{student});
                
                studentAgent.start();
                
            }
        }
        catch (
                Exception e) {
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
