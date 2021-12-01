import Ontology.Elements.Module;
import Ontology.Elements.Preference;
import Ontology.Elements.Student;
import Ontology.Elements.StudentTimetablePreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class StudentGeneration
{
    
    public static Student generateRandomStudent() {
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
    
    public static void randomlyAssignModulesToStudent(Student student, List<Module> modules, int modulesPerStudent) {
        if (modulesPerStudent > modules.size()) {
            modulesPerStudent = modules.size();
        }
        var r = new Random();
        var studentModuleIds = new ArrayList<String>();
        for (int i = 0; i < modulesPerStudent; i++) {
            int m = r.nextInt(modules.size());
            studentModuleIds.add(modules.get(m).getModuleId());
        }
        
        student.setModuleIds(studentModuleIds);
        //check that this is actually setting the student lol
    }
    
   
}
