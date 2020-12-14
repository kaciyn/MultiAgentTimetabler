package Ontology.Elements.Concepts;

import jade.core.AID;

import java.util.ArrayList;

public class Module extends Identifiable
{
    private ArrayList<AID> students;
    private ArrayList<Tutorial> tutorials;

    public ArrayList<AID> getStudents()
    {
        return students;
    }

    public void setStudents(ArrayList<AID> students)
    {
        this.students = students;
    }

    public ArrayList<Tutorial> getTutorials()
    {
        return tutorials;
    }

    public void setTutorials(ArrayList<Tutorial> tutorials)
    {
        this.tutorials = tutorials;
    }
}
