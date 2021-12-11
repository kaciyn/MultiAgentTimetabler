import Ontology.Elements.Module;
import Ontology.Elements.TutorialTimetable;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TimetableGeneration
{
    static TutorialTimetable generateRandomTimetable(List<Module> modules) {
        var emptyTimeslotIds = IntStream.range(1, 45).boxed().collect(Collectors.toList());
        
        var timetable = new TutorialTimetable();
        
        var r = new Random();
        
        //assigns each tutorial to random timeslot, removing timeslot after assignation
        modules.forEach((module) -> {
            module.getTutorials().forEach((tutorial -> {
                int i = r.nextInt(emptyTimeslotIds.size()) + 1;
                var timeslotId = (int) emptyTimeslotIds.get(i);
                
                tutorial.setTimeSlotId(timeslotId);
                
                timetable.set(timeslotId, tutorial);
                
                emptyTimeslotIds.remove(i);
            }));
        });
        return timetable;
    }
}