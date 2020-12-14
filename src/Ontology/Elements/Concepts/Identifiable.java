package Ontology.Elements.Concepts;

import jade.content.Concept;

public class Identifiable implements Concept
{
    private int id;


    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
}
