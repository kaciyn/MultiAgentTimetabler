//import Ontology.Elements.Preference;
//import Ontology.Elements.Student;
//import Ontology.Elements.StudentTimetablePreferences;
//
//import java.util.ArrayList;
//import java.util.concurrent.ThreadLocalRandom;
//
//public class StudentGeneration
//{
//    private ArrayList<Integer>existingMatrics;
//
//    public static Student generateRandomStudent() {
//        var student = new Student();
//        student.setMatriculationNumber(generateRandomStudentMatriculationNumber());
//        student.setStudentTimetablePreferences(generateRandomStudentPreferences());
//        return student;
//    }
//
//    public static int generateRandomStudentMatriculationNumber(ArrayList<Integer>existingMatrics) {
//        var newMatric=4000000 + (int) (ThreadLocalRandom.current().nextFloat() * 900000);
//
//        while(existingMatrics.contains(newMatric)){
//            newMatric=4000000 + (int) (ThreadLocalRandom.current().nextFloat() * 900000);
//        }
//
//         return newMatric;
//    }
//
//    public static StudentTimetablePreferences generateRandomStudentPreferences() {
//        var studentPreferences = new StudentTimetablePreferences();
//        for (int i = 1; i < 45; i++) {
//            var preference = Helpers.getLinearRandomNumber(4);
//
//            //linear random distribution, from most likely to least: no pref,prefer, prefer not, cannot
//            //this is fairly arbitrary but didn't think a student would/should be equally likely to not be able attend slots at the same probability as everything
//            switch (preference) {
//                case 1:
//                    studentPreferences.set(i, Preference.NO_PREFERENCE);
//                    break;
//                case 2:
//                    studentPreferences.set(i, Preference.PREFER);
//                    break;
//                case 3:
//                    studentPreferences.set(i, Preference.PREFER_NOT);
//                    break;
//                case 4:
//                    studentPreferences.set(i, Preference.CANNOT);
//                    break;
//            }
//        }
//
//        return studentPreferences;
//    }
//
//}
