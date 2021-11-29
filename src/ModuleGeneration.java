import Ontology.Elements.Concepts.Module;
import Ontology.Elements.Concepts.Tutorial;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ModuleGeneration
{
    public static List<Module> initialiseModules(int numberOfModules, int tutorialGroupsPerModule, int numberOfStudents) {
        var modules = new ArrayList<Module>();
        
        //for randomised tutorial group amounts
        if (tutorialGroupsPerModule <= 0) {
            for (int i = 0; i < numberOfModules; i++) {
                tutorialGroupsPerModule = ThreadLocalRandom.current().nextInt(1, 5 + 1);
                var module = ModuleGeneration.generateRandomModuleWithStudents(tutorialGroupsPerModule, numberOfStudents);
                module.setTutorialGroupAmount(ThreadLocalRandom.current().nextInt(1, 5 + 1));
                modules.add(module);
            }
        }
        else {
            for (int i = 0; i < numberOfModules; i++) {
                var module = ModuleGeneration.generateRandomModuleWithStudents(tutorialGroupsPerModule, numberOfStudents);
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
    
 
    public static Module generateRandomModuleWithStudents(int tutorialGroups, int numberOfEnrolledStudents) {
        var moduleId = generateRandomModuleId();
        
        var evenTutorialSize = numberOfEnrolledStudents / tutorialGroups;
        
        var tutorials = new ArrayList<Tutorial>();
        
        for (int i = 0; i < tutorialGroups; i++) {
            tutorials.add(new Tutorial(moduleId, (evenTutorialSize)));
        }
        
        if (numberOfEnrolledStudents % 2 != 0) {
            tutorials.get(tutorialGroups - 1).setCapacity(evenTutorialSize + 1);
        }
        
        return new Module(moduleId, tutorialGroups, tutorials);
    }
    
    public static Module generateRandomModule(int tutorialGroups) {
        var moduleId = generateRandomModuleId();
        
        var tutorials = new ArrayList<Tutorial>();
        
        for (int i = 0; i < tutorialGroups; i++) {
            tutorials.add(new Tutorial(moduleId));
        }
        
        return new Module(moduleId, tutorialGroups, tutorials);
    }
    
}
