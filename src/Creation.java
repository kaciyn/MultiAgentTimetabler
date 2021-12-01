import Ontology.Elements.Module;
import Ontology.Elements.Student;
import Ontology.Elements.TutorialTimetable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Creation
{
    
    public static void initialise(int numberOfStudents, int numberOfModules, int tutorialGroupsPerModule, int modulesPerStudent) {
        var modules = ModuleGeneration.initialiseModules(numberOfModules, tutorialGroupsPerModule, numberOfStudents);
        var timetable = generateRandomTimetable(modules);
        
        var students = new ArrayList<Student>();
        for (int i = 0; i < numberOfStudents; i++) {
            var student = StudentGeneration.generateRandomStudent();
            StudentGeneration.randomlyAssignModulesToStudent(student, modules, modulesPerStudent);
            students.add(student);
        }
        
    }
    
    private static TutorialTimetable generateRandomTimetable(List<Module> modules) {
        var emptyTimeslotIds = IntStream.range(1, 45).boxed().collect(Collectors.toList());
        
        var timetable = new TutorialTimetable();
        
        var r = new Random();
        
        //assigns each tutorial to random timeslot, removing timeslot after assignation
        modules.forEach((module) -> {
            module.getTutorials().forEach((tutorial -> {
                int i = r.nextInt(emptyTimeslotIds.size()) + 1;
                var timeslotId = (int)emptyTimeslotIds.get(i);
                
                tutorial.setTimeSlotId(timeslotId);
                
                timetable.set(timeslotId, tutorial);
                
                emptyTimeslotIds.remove(i);
            }));
        });
        return timetable;
    }
}
