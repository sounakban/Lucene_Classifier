/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TesterClasses;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 *
 * @author sounakBanerjee
 */

class test {
    int a;
}

public class Tester2 {
    
    
    
    public static void main(String[] args) {
        
        
        //Access Class Variables
        HashMap<String, test> testMap= new HashMap();
        test t1 = new test();
        t1.a = 3;
        testMap.put("t", t1);
        t1.a = 5;
        System.out.println("result : " + testMap.get("t").a);
        
        
        
        //Sorting a Map
        Map<String, Integer> unsortedMap = new HashMap();
        unsortedMap.put("x", 4);
        unsortedMap.put("w", 5);
        unsortedMap.put("z", 2);
        unsortedMap.put("y", 3);
        Map<String, Integer> sortedMap =
                unsortedMap.entrySet().stream()
                        .sorted(Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                                (e1, e2) -> e2, LinkedHashMap::new));
        for(Map.Entry<String, Integer> ent : sortedMap.entrySet()) {
            System.out.println(ent.getKey() + " : " + ent.getValue());
        }
        
        System.out.println(Math.log(Double.MIN_VALUE));
        
    }
}
