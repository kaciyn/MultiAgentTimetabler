package Ontology.Elements;

import java.util.ArrayList;

public class Tutorial extends ModuleEvent
{
    private ArrayList<Student> students;
    
    //TODO MAYBE THE TUTORIAL SHOULDN'T HAVE ITS STUDENT LIST PASSED AROUND? GDPR? like should it just be the timetabler agent that has the info
    //todo get everything else working then maybe we can move it out
    
    public ArrayList<Student> getStudents() {
        return students;
    }
    
    public void setStudents(ArrayList<Student> students) {
        this.students = students;
    }
    
}
