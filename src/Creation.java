import Ontology.Elements.Concepts.Module;

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
}
