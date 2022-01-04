import Agents.StudentAgent;
import Agents.TimetablerAgent;
import FIPA._MTSImplBase;
import Ontology.Elements.Module;
import Ontology.Elements.Preference;
import Ontology.Elements.Student;
import Ontology.Elements.StudentTimetablePreferences;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Application
{
    private static int numberOfModules;
    private static int tutorialGroupsPerModule;
    private static int numberOfStudents;
    private static int modulesPerStudent;
    
    private static ArrayList<Module> modules;
    private static ArrayList<Student> students;
    
    private static ArrayList<Integer> existingMatrics;
    
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
        students = new ArrayList<Student>();
        modules = new ArrayList<Module>();
        modules = ModuleGeneration.initialiseModules(numberOfModules, tutorialGroupsPerModule, numberOfStudents);
        existingMatrics = new ArrayList<Integer>();
        
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
                int m = r.nextInt(modules.size());
                //add random module to student
                studentModuleIds.add(modules.get(m).getModuleId());
                //add student to module
                modules.get(m).addEnrolledStudentId(student.getMatriculationNumber());
                
                //assign student to random module tutorial
//                for (int k = 0; k < modules.get(m).getTutorialGroupAmount(); k++) {
                    int t = r.nextInt(modules.get(m).getTutorialGroupAmount());
                    var d = modules.get(m).getTutorials().get(t).getStudents().size();
                    //if tutorial full get another random tutorial
                    while (modules.get(m).getTutorials().get(t).getStudents().size() == modules.get(m).getTutorials().get(t).getCapacity()) {
                        t = r.nextInt(modules.get(m).getTutorialGroupAmount());
                        
                    }
                    //add tutorial to student
                    student.addTutorial(modules.get(m).getTutorials().get(t));
                    //add student to tutorial
                    modules.get(m).getTutorials().get(t).addStudent(student);
                    
                }
            }
            //foreach student foreach module assign random tutorial if tutorial not full
            
            student.setModuleIds(studentModuleIds);
            
            students.add(student);
        }
        
        for (int j = 0; j < numberOfStudents; j++) {
            for (int i = 0; i < numberOfStudents; i++) {
                if (students.get(i).getMatriculationNumber() == students.get(j).getMatriculationNumber() && i != j) {
                    System.out.println("student " + students.get(i).getMatriculationNumber() + " and student " + students.get(j).getMatriculationNumber());
                }
                
            }
        }

//        for (int i = 0; i < modules.size(); i++) {
//            var r = new Random();
//
//            modules.get(i).getTutorials().forEach(tutorial -> {
//
//                var studentsInTutorial = new ArrayList<Student>();
//
//                //keep adding random students on module until tutorial is full
//                while (studentsInTutorial.size() < tutorial.getCapacity()) {
//
//                    var studentsWithoutModuleTutorial=(ArrayList<Student>)students.stream().filter( student->
//                    student.getTutorials().stream().filter(assignedTutorial -> tutorial.getModuleId() == assignedTutorial.getModuleId()).count()<1).collect(Collectors.toList());
//    //TODO JUST DO IT BY MATRIC????idk bro
//                    //UGH NO
//                    //idk i guess go by student then?
//
//                    var randomStudent = studentsWithoutModuleTutorial.get(r.nextInt(studentsWithoutModuleTutorial.size()));
//                    //get a new student if student is already assigned a tutorial on module
//                    if (randomStudent.getTutorials() != null) {
//                        try {
//
//
//                            var assignedTutorials = randomStudent.getTutorials();
//                            while (assignedTutorials.stream().filter(assignedTutorial -> tutorial.getModuleId() == assignedTutorial.getModuleId()).count()>0) {
//                                randomStudent = students.get(r.nextInt(students.size()));
//                            }
//                        }
//                        catch (Exception e) {
//                            System.out.println(e);
//                            System.out.println(randomStudent.getTutorials() == null);
//                        }
//                    }
//
//                    studentsInTutorial.add(randomStudent);
//                    var studentmatric=
//                   students.stream().filter(student -> student.getMatriculationNumber()==randomStudent.getMatriculationNumber()).findFirst();
//                    randomStudent.addTutorial(tutorial);
//
//                }
//                tutorial.setStudents(studentsInTutorial);
//            });
////            tutorialedStudents.addAll(ModuleGeneration.randomlyAssignStudentsToTutorials(modules.get(i), ));
//
//        }

//        var tutorialedStudents = new ArrayList<Student>();
        
        var myContainer = myRuntime.createMainContainer(myProfile);
        
        try {
            
            //start agent controller (also an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();
            
            AgentController timetablerAgent = myContainer.createNewAgent("timetabler", TimetablerAgent.class.getCanonicalName(), new Object[]{timetable, students});
            timetablerAgent.start();
            
            for (int i = 0; i < students.size(); i++) {
                var student = students.get(i);
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
    
    private static Student generateRandomStudent() {
        var student = new Student();
        student.setMatriculationNumber(generateRandomStudentMatriculationNumber());
        student.setStudentTimetablePreferences(generateRandomStudentPreferences());
        return student;
    }
    
    private static int generateRandomStudentMatriculationNumber() {
        var newMatriculationNumber = 4000000 + (int) (ThreadLocalRandom.current().nextFloat() * 900000);
        
        while (existingMatrics.contains(newMatriculationNumber)) {
            newMatriculationNumber = 4000000 + (int) (ThreadLocalRandom.current().nextFloat() * 900000);
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
                    studentPreferences.set(i, Preference.NO_PREFERENCE);
                    break;
                case 2:
                    studentPreferences.set(i, Preference.PREFER);
                    break;
                case 3:
                    studentPreferences.set(i, Preference.PREFER_NOT);
                    break;
                case 4:
                    studentPreferences.set(i, Preference.CANNOT);
                    break;
            }
        }
        
        return studentPreferences;
    }
    
}
