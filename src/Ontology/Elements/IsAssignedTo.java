package Ontology.Elements;

public class IsAssignedTo
{
    private Student attendingStudent;
    private Module tutorial;
    
    public Student getAttendingStudent() {
        return attendingStudent;
    }
    
    public void setAttendingStudent(Student attendingStudent) {
        this.attendingStudent = attendingStudent;
    }
    
    public Module getTutorial() {
        return tutorial;
    }
    
    public void setTutorial(Module tutorial) {
        this.tutorial = tutorial;
    }
}
