package Ontology.Elements.Concepts;

import jade.content.Concept;

public class Room implements Concept
{
    private int id;
    private int capacity;

    public int getCapacity()
    {
        return capacity;
    }

    public void setCapacity(int capacity)
    {
        this.capacity = capacity;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
}
