package Agents;

import Ontology.Elements.Concepts.Timetable;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashSet;
import java.util.Hashtable;

public class TimetablerAgent extends Agent
{

    private Hashtable<Integer, Item> catalogue;
    private HashSet<AID> studentAgents;
    //we should already know the student ids prior to their agents starting up
    private HashSet<String> studentIDs;
    private int currentItemIndex;

    protected void setup()
    {
        studentAgents = new HashSet<>();

        // Register the auctioneer in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("timetabler");
        sd.setName("timetabler");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
//get args
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            catalogue = (Hashtable) args[0];

            System.out.println("Catalogue loaded");
        }
        else {
// Make the agent terminate immediately
            System.out.println("No catalogue found");
            doDelete();
        }

        System.out.println("Waiting for buyer registration...");

        addBehaviour(new AuctionRegistrationReceiver());

        addBehaviour(new WakerBehaviour(this, 6000)
        {
            protected void handleElapsedTimeout()
            {
                System.out.println("Starting auction");
                currentItemIndex = 1;
                addBehaviour(new AuctionServer());
            }
        });

    }

    //receives bidder registrations for auction
    private class AuctionRegistrationReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);


            if (msg != null && msg.getConversationId().equals("register-with-timetabler")) {

                AID newStudentAID = msg.getSender();
                studentAgents.add(newStudentAID);

                var timetable=new Timetable();

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Registration confirmed");

                System.out.println(newStudentAID.getName() + " registered for auction");

                myAgent.send(reply);
            }
            else {
//                System.out.println("Unknown/null message received");
                block();
            }

        }


    }

    private class AuctionServer extends Behaviour
    {
        private AID highestBidder = null; // The agent who provides the best offer
        private int highestBid = 0; // The best offered price
        int responseCount = 0; // The counter of replies from seller agents                    // Receive all bids/refusals from bidder

        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        Item item;

        public void action()
        {
            switch (step) {
                case 0:
                    item = catalogue.get(currentItemIndex);
                    System.out.println(currentItemIndex);

                    System.out.println("Auctioning item: " + item.Description);

                    // Send the cfp to all bidders
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
//iterate over bidder hashset
                    for (AID bidder : studentAgents) {
                        cfp.addReceiver(bidder);
                    }

                    cfp.setContent(item.Description);
                    cfp.setConversationId("auction-item");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value myAgent.send(cfp);
// Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bid-on-item"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    myAgent.send(cfp);

                    step = 1;
                    break;
                case 1:
                    // The counter of replies from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) { // This is an offer
                            int bid = Integer.parseInt(reply.getContent());
                            if (highestBidder == null || bid > highestBid) {
                                // This is the highest bid at present
                                highestBid = bid;
                                highestBidder = reply.getSender();
                            }
                        }
                        else {
                            reply.getPerformative();
                        }
                        responseCount++;
                        if (responseCount >= studentAgents.size()) {
                            // received all responses
                            if (highestBidder == null || highestBid < item.StartingPrice) {
                                step = 3;
                            }
                            else {
                                step = 2;

                            }
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    // Send confirmation to bidder
                    ACLMessage bidConfirmation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    bidConfirmation.addReceiver(highestBidder);
                    bidConfirmation.setContent(item.Description + "," + highestBid);
                    bidConfirmation.setConversationId("bid-successful");
                    bidConfirmation.setReplyWith("win" + System.currentTimeMillis());
                    myAgent.send(bidConfirmation);

                    System.out.println(item.Description + " has been bought by: " + highestBidder + " for " + highestBid);

                    incrementItem();

                    break;
                case 3:
                    //unsold item
                    System.out.println(item.Description + " has not been bid for, or has not met starting price. Auctioning next item");

                    incrementItem();
                    break;
                case 4:
                    //end auction
                    System.out.println("All items bid for. Auction concluded");

                    // Send the cfp to all bidders
                    ACLMessage endNotification = new ACLMessage(ACLMessage.INFORM);
//iterate over bidder hashset
                    for (AID bidder : studentAgents) {
                        endNotification.addReceiver(bidder);
                    }

                    endNotification.setConversationId("auction-concluded");
                    myAgent.send(endNotification);


                    myAgent.doDelete();
                    step = 5;
                    break;

            }

        }

        //resets for next item, ends auction if at end of catalogue
        void incrementItem()
        {
            //reset for next item
            responseCount = 0;
            highestBidder = null; // The agent who provides the best offer
            highestBid = 0; // The best offered price
            currentItemIndex++;

            if (currentItemIndex >= catalogue.size()) {
                step = 4;

            }
            else {
                step = 0;
            }

        }

        public boolean done()
        {
            return (step == 5);
        }
    }


    protected void takeDown()
    {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Auctioneer " + getAID().getName() + " terminating.");
    }
}


