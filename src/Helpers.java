import java.util.Random;

public class Helpers {
    //cred: Briguy37 @ https://stackoverflow.com/questions/5969447/java-random-integer-with-non-uniform-distribution
    public static int getLinearRandomNumber(int maxSize){
        //Get a linearly multiplied random number
        int randomMultiplier = maxSize * (maxSize + 1) / 2;
        Random r=new Random();
        int randomInt = r.nextInt(randomMultiplier);
        
        //Linearly iterate through the possible values to find the correct one
        int linearRandomNumber = 0;
        for(int i=maxSize; randomInt >= 0; i--){
            randomInt -= i;
            linearRandomNumber++;
        }
        
        return linearRandomNumber;
    }
}
