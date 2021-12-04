package Ontology.Elements;

import jade.content.Predicate;

public class IsEnrolledOn implements Predicate
{
    private Student enrolledStudent;
    private Module module;
    
    public Student getEnrolledStudent() {
        return enrolledStudent;
    }
    
    public void setEnrolledStudent(Student enrolledStudent) {
        this.enrolledStudent = enrolledStudent;
    }
    
    public Module getModule() {
        return module;
    }
    
    public void setModule(Module module) {
        this.module = module;
    }
}
