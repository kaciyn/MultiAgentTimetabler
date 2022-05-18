import java.util.*;
import java.util.stream.Collectors;

public class Helpers
{
    //cred: Briguy37 @ https://stackoverflow.com/questions/5969447/java-random-integer-with-non-uniform-distribution
    public static int getLinearRandomNumber(int maxSize) {
        //Get a linearly multiplied random number
        int randomMultiplier = maxSize * (maxSize + 1) / 2;
        Random r = new Random();
        int randomInt = r.nextInt(randomMultiplier);
        
        //Linearly iterate through the possible values to find the correct one
        int linearRandomNumber = 0;
        for (int i = maxSize; randomInt >= 0; i--) {
            randomInt -= i;
            linearRandomNumber++;
        }
        
        return linearRandomNumber;
    }
    
    //modified from: https://www.geeksforgeeks.org/sorting-a-hashmap-according-to-values/
    // function to sort hashmap by values
    public static HashMap<Object, Integer>
    sortByValueAscending(HashMap<Object, Integer> hashMap)
    {
        
        return hashMap.entrySet()
                      .stream()
                      .sorted(Comparator.comparing(Map.Entry::getValue))
                      .collect(Collectors.toMap(
                              Map.Entry::getKey,
                              Map.Entry::getValue,
                              (e1, e2) -> e1, LinkedHashMap::new));
    }
    
    public static HashMap<Object, Integer>
    sortByValueDescending(HashMap<Object, Integer> hashMap)
    {
        
        return hashMap.entrySet()
                      .stream()
                      .sorted(Comparator.comparing(Map.Entry::getValue))
                      .collect(Collectors.toMap(
                              Map.Entry::getKey,
                              Map.Entry::getValue,
                              (e1, e2) -> e2, LinkedHashMap::new));
    }
    
    
    
 
    
}
