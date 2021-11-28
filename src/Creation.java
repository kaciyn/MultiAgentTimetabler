import Ontology.Elements.Concepts.Module;
import Ontology.Elements.Concepts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Creation
{
    
    public static void initialiseTimetable(int numberOfModules, int tutorialGroupsPerModule, int numberOfEnrolledStudents) {
        var modules = initialiseModules(numberOfModules, tutorialGroupsPerModule, numberOfEnrolledStudents);
        
        generateRandomTimetable(modules);
        
    }
    
    public static List<Module> initialiseModules(int numberOfModules, int tutorialGroupsPerModule, int numberOfEnrolledStudents) {
        var modules = new ArrayList<Module>();
        
        //for randomised tutorial group amounts
        if (tutorialGroupsPerModule <= 0) {
            for (int i = 0; i < numberOfModules; i++) {
                tutorialGroupsPerModule = ThreadLocalRandom.current().nextInt(1, 5 + 1);
                var module = generateRandomModuleWithStudents(tutorialGroupsPerModule, numberOfEnrolledStudents);
                module.setTutorialGroupAmount(ThreadLocalRandom.current().nextInt(1, 5 + 1));
                modules.add(module);
            }
        }
        else {
            for (int i = 0; i < numberOfModules; i++) {
                var module = generateRandomModuleWithStudents(tutorialGroupsPerModule, numberOfEnrolledStudents);
                module.setTutorialGroupAmount(tutorialGroupsPerModule);
                modules.add(module);
            }
        }
        return modules;
    }
    
    public static Student generateRandomStudent() {
        for (int i = 0; i < 2; i++) {
        
        }
        return new Student(generateRandomStudentId(), generateRandomStudentPreferences());
    }
    
    public static int generateRandomStudentId() {
        return 10000000 + (int) (ThreadLocalRandom.current().nextFloat() * 900000);
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
    
    private static void generateRandomTimetable(List<Module> modules) {
        var emptyTimeslotIds = IntStream.range(1, 45).boxed().collect(Collectors.toList());
        
        var timetable = new TrimesterTimetable();
        
        var r = new Random();
        
        //assigns each tutorial to random timeslot, removing timeslot after assignation
        modules.forEach((module) -> {
            module.getTutorials().forEach((tutorial -> {
                int i = r.nextInt(44) + 1;
                var timeslotId = emptyTimeslotIds.get(i);
                
                tutorial.setTimeSlotId(timeslotId);
                
                timetable.set(timeslotId, tutorial);
                
                emptyTimeslotIds.remove(i);
            }));
        });
        
    }
}
