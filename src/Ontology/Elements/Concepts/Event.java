package Ontology.Elements.Concepts;

import java.util.ArrayList;

public class Event extends Identifiable
{
   private Room room;
   
   private Timeslot timeslot;
   
    public Room getRoom() {
        return room;
    }
    
    public void setRoom(Room room) {
        this.room = room;
    }
    
    public Timeslot getTimeslot() {
        return timeslot;
    }
    
    public void setTimeslot(Timeslot timeslot) {
        this.timeslot = timeslot;
    }
}
