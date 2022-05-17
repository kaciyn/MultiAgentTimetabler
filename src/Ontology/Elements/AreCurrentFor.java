package Ontology.Elements;

import jade.content.onto.annotations.Slot;
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
    
    @Slot(mandatory = true)
    private StudentStatistics studentStats;
    @Slot(mandatory = true)
    private AID student;
}
