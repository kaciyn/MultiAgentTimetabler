package Ontology.Elements;

import jade.content.Predicate;
import jade.core.AID;


public class IsRegisteringWith implements Predicate
{
    AID student;
    
    AID service;
    
    public IsRegisteringWith(AID student, AID service) {
        this.student = student;
        this.service = service;
    }
}
