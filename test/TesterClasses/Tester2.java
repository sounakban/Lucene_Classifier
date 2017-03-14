/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TesterClasses;

import java.util.HashMap;

/**
 *
 * @author sounakBanerjee
 */

class test {
    int a;
}

public class Tester2 {
    
    
    
    public static void main(String[] args) {
        
        HashMap<String, test> testMap= new HashMap();
        test t1 = new test();
        t1.a = 3;
        testMap.put("t", t1);
        t1.a = 5;
        System.out.println("result : " + testMap.get("t").a);
    }
}
