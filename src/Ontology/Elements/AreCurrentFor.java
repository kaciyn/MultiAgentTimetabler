package Ontology.Elements;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class AreCurrentFor implements Predicate
{
    public StudentAgentMetrics getStudentMetrics() {
        return studentMetrics;
    }
    
    public void setStudentMetrics(StudentAgentMetrics studentMetrics) {
        this.studentMetrics = studentMetrics;
    }
    
    public AID getStudent() {
        return student;
    }
    
    public void setStudent(AID student) {
        this.student = student;
    }
    
    @Slot(mandatory = true)
    private StudentAgentMetrics studentMetrics;
    @Slot(mandatory = true)
    private AID student;
}
