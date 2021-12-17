package Ontology.Elements;

import jade.core.behaviours.ParallelBehaviour;

public class SomeParallelBehaviour extends ParallelBehaviour
{
    
    public SomeParallelBehaviour() {
        super(ParallelBehaviour.WHEN_ALL);
    }
    
    @Override
    public void onStart() {
        for (int i = 0; i < 3; i++) {
            addSubBehaviour(new RandomTimeOut(this.myAgent,
                                              ThreadLocalRandom.current().nextInt(10000, 20000), i + 1));
        }
    }
    
    private class RandomTimeOut extends WakerBehaviour
    {
        
        private final int _id;
        
        public RandomTimeOut(final Agent a, final int time, final int id) {
            super(a, time);
            
            _id = id;
        }
        
        @Override
        protected void handleElapsedTimeout() {
            System.out.println(String.format("Behaviour %s Executed!", _id));
        }
    }
    
    @Override
    public int onEnd() {
        System.out.println("Parallel behaviour terminated!");
        return super.onEnd();
    }
}