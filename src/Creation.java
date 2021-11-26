import Ontology.Elements.Concepts.Module;
import Ontology.Elements.Concepts.Timetable;
import Ontology.Elements.Concepts.Tutorial;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Creation
{
    
    public static void initialiseTimetable(int numberOfModules, int tutorialGroupsPerModule) {
        initialiseModules(numberOfModules, tutorialGroupsPerModule);
        
    }
    
    public static void initialiseModules(int numberOfModules, int tutorialGroupsPerModule) {
        var modules = new ArrayList<Module>();
        
        //for randomised tutorial group amounts
        if (tutorialGroupsPerModule <= 0) {
            for (int i = 0; i < numberOfModules; i++) {
                var module = generateRandomModule();
                module.setTutorialGroupAmount(ThreadLocalRandom.current().nextInt(1, 5 + 1));
                modules.add(module);
            }
        }
        else {
            for (int i = 0; i < numberOfModules; i++) {
                var module = generateRandomModule();
                module.setTutorialGroupAmount(tutorialGroupsPerModule);
                modules.add(module);
            }
        }
    }
    
    public static Module generateRandomModule() {
        var moduleId = "SET";
        int moduleNumber = 100000 + (int) (ThreadLocalRandom.current().nextFloat() * 900000);
        moduleId += String.valueOf(moduleNumber);
        
        return new Module(moduleId);
    }
    
    
    public static String generateRandomModuleId() {
        var moduleId = "SET";
        int moduleNumber = 100000 + (int) (ThreadLocalRandom.current().nextFloat() * 900000);
        moduleId += String.valueOf(moduleNumber);
        
        return moduleId;
    }
    
    public static Module generateRandomModuleWithStudents(int tutorialGroups,int numberOfEnrolledStudents) {
       
        var evenTutorialSize=numberOfEnrolledStudents/tutorialGroups;
        var tutorials= new ArrayList<Tutorial>();
        for (int i = 0; i < tutorialGroups; i++) {
            tutorials.add(new Tutorial())
        }
        if (numberOfEnrolledStudents%2!=0){
        
        }
        
        return new Module(moduleId);
    }
    
    public static Module generateRandomModuleWithStudents(ArrayList<String> studentIds) {
        var numberOfEnrolledStudents = studentIds.size();
        
       
        return new Module(moduleId);
    }
    
    
    
    private static generateRandomTimetable(ArrayList<Module> modules) {
        for (module:
             modules) {
            
        }
        var tutorials = new ArrayList<Tutorial>()
        var timetable = new Timetable();
        
        for (int i = 1; i <= 45; i++) {
        
        }
        
    }
}
