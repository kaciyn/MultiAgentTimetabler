package Models;

import Ontology.Elements.Concepts.TutorialSlot;
import jade.content.Concept;

import java.time.DayOfWeek;
import java.util.ArrayList;

public class Tutorial implements Concept
{
    protected int id;
    
    protected String moduleId;
    
    protected Long capacity;
    
    protected Long timeslotId;
    
    private Long startHour;
    
    private DayOfWeek day;
    
    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }
    
    private ArrayList<Long> attendingStudentIds;
    
    public Long getCapacity() {
        return capacity;
    }
    
    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }
    
    public Long getTimeslotId() {
        return timeslotId;
    }
    
    public void setTimeSlotId(Long timeslotId) {
        this.timeslotId = timeslotId;
    }
    
    public DayOfWeek getDay() {
        switch ((int) (this.timeslotId / 10)) {
            case 0:
                return DayOfWeek.MONDAY;
            case 1:
                return DayOfWeek.TUESDAY;
            case 2:
                return DayOfWeek.WEDNESDAY;
            case 3:
                return DayOfWeek.THURSDAY;
            case 4:
                return DayOfWeek.FRIDAY;
            default:
                throw new IllegalArgumentException("Invalid timeslotId");
        }
    }
    
    public Long getStartHour()
    {
        this.startHour = (this.timeslotId % 10) + 8;
        return this.startHour;
    }
    
    //kind of unnecessary since we can just ref with module and timeslotid
    public String getId() {
        return moduleId + "-T-" + timeslotId;
    }
    
    public String getModuleId() {
        return moduleId;
    }
    
    public ArrayList<Long> getStudentIds() {
        if (attendingStudentIds == null) {
            attendingStudentIds = new ArrayList<Long>();
        }
        return attendingStudentIds;
    }
    
    public void setStudents(ArrayList<Student> students) {
        var studentIds = new ArrayList<Long>();
        students.forEach(student -> {
            studentIds.add(student.getMatriculationNumber());
        });
        
        this.attendingStudentIds = studentIds;
    }
    
    public void addStudent(Student student) {
        this.attendingStudentIds.add(student.getMatriculationNumber());
    }
    
    public void setStudentIds(ArrayList<Long> studentIds) {
        
        this.attendingStudentIds = studentIds;
    }
    
    public void addStudentId(Long studentId) {
        this.attendingStudentIds.add(studentId);
    }
    
    public TutorialSlot getTutorialSlot() {
        return new TutorialSlot(this.moduleId, this.timeslotId);
    }
}

