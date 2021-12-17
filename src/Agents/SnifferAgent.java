package Agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;

public class SnifferAgent
{
    protected void setup()
    {
    }

    public class B1 extends Behaviour
    {
        private int timesCalled = 0;

        public B1(Agent a)
        {
            myAgent = a;
        }

        @Override
        public void action()
        {
            System.out.println(myAgent.getLocalName());
            timesCalled++;
        }

        @Override
        public boolean done()
        {
            return timesCalled >= 10;
        }
    }
    
    protected void takeDown()
    {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        System.out.println("Timetabler " + getAID().getName() + " terminating.");
    }
}
