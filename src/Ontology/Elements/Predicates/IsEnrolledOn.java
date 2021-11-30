package Ontology.Elements.Predicates;

import Ontology.Elements.Concepts.Student;

public class IsEnrolledOn
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
