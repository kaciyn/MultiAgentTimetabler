package Generation;

import Models.Tutorial;
import Models.Module;
import Models.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ModuleGeneration
{
 
    public static ArrayList<Module> initialiseModules(int numberOfModules, int tutorialGroupsPerModule, int numberOfStudents) {
        var modules = new ArrayList<Module>();
        
        //for randomised tutorial group amounts
        if (tutorialGroupsPerModule <= 0) {
            for (int i = 0; i < numberOfModules; i++) {
                tutorialGroupsPerModule = ThreadLocalRandom.current().nextInt(1, 5 + 1);
                var module = ModuleGeneration.generateRandomModuleWithStudentAmounts(tutorialGroupsPerModule, numberOfStudents);
                module.setTutorialGroupAmount(ThreadLocalRandom.current().nextInt(1, 5 + 1));
                modules.add(module);
            }
        }
        else {
            for (int i = 0; i < numberOfModules; i++) {
                var module = ModuleGeneration.generateRandomModuleWithStudentAmounts(tutorialGroupsPerModule, numberOfStudents);
                module.setTutorialGroupAmount(tutorialGroupsPerModule);
                modules.add(module);
            }
        }
        return modules;
    }
    
    public static String generateRandomModuleId() {
        var moduleId = "SET";
        int moduleNumber = 100000 + (int) (ThreadLocalRandom.current().nextFloat() * 900000);
        moduleId += String.valueOf(moduleNumber);
        
        return moduleId;
    }
    
    public static Module generateRandomModuleWithStudentAmounts(int tutorialGroups, int numberOfEnrolledStudents) {
        var moduleId = generateRandomModuleId();
        
        var evenTutorialSize = numberOfEnrolledStudents / tutorialGroups;
        
        ArrayList<Tutorial> tutorials = new ArrayList<Tutorial>();
        
        for (int i = 0; i < tutorialGroups; i++) {
            var tut = new Tutorial();
            tut.setModuleId(moduleId);
            tut.setCapacity((long) evenTutorialSize);
            tutorials.add(tut);
            
        }
        
        if (numberOfEnrolledStudents % 2 != 0) {
            tutorials.get(tutorialGroups - 1).setCapacity((long) (evenTutorialSize + 1));
        }
        
        var module = new Module();
        module.setModuleId(moduleId);
        module.setTutorialGroupAmount(tutorialGroups);
        module.setTutorials(tutorials);
        return module;
    }
    
    public static Module generateRandomModule(int tutorialGroups) {
        var moduleId = generateRandomModuleId();
        
        var tutorials = new ArrayList<Tutorial>();
        
        for (int i = 0; i < tutorialGroups; i++) {
            var tutorial = new Tutorial();
            tutorial.setModuleId(moduleId);
            
            tutorials.add(tutorial);
        }
        var module = new Module();
        module.setModuleId(moduleId);
        module.setTutorialGroupAmount(tutorialGroups);
        module.setTutorials(tutorials);
        return module;
    }
    
    public static List<Student> randomlyAssignStudentsToTutorials(Module module, List<Student> studentsInModule) {
        var r = new Random();
        var assignedStudentsInModule = new ArrayList<Student>();
        module.getTutorials().forEach(tutorial -> {
            
            var studentsInTutorial = new ArrayList<Student>();
            
            //keep adding random students on module until tutorial is full
            while (studentsInTutorial.size() <= tutorial.getCapacity()) {
                var randomStudent = studentsInModule.get(r.nextInt(studentsInModule.size()));
                studentsInTutorial.add(randomStudent);
                randomStudent.addTutorialSlot(tutorial.getTutorialSlot());
                
                assignedStudentsInModule.add(randomStudent);
            }
            
        });
        return assignedStudentsInModule;
    }
}

