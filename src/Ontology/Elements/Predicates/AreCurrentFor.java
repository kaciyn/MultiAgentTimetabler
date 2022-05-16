package Ontology.Elements.Predicates;

import Ontology.Elements.Concepts.StudentStatistics;
import jade.core.AID;

public class AreCurrentFor
{
    public StudentStatistics getStudentStats() {
        return studentStats;
    }
    
    public void setStudentStats(StudentStatistics studentStats) {
        this.studentStats = studentStats;
    }
    
    public AID getStudent() {
        return student;
    }
    
    public void setStudent(AID student) {
        this.student = student;
    }
    
    private StudentStatistics studentStats;
    
    private AID student;
    
//    public boolean isFinalStats() {
//        return finalStats;
//    }
//
//    public void setFinalStats(boolean finalStats) {
//        this.finalStats = finalStats;
//    }
//
//    private boolean finalStats;
}
