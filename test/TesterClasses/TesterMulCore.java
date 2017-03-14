/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package TesterClasses;

/**
 *
 * @author sounakBanerjee
 */
import java.util.*;
import java.util.concurrent.*;

public class TesterMulCore {
    
    public static class WordLengthCallable
            implements Callable {
        private String word;
        public WordLengthCallable(String word) {
            System.out.println("Start : " + word);
            this.word = word;
        }
        public Integer call() {
            System.out.println("End : " + word);
            return word.length();
        }
    }
    
    public static void main(String args[]) throws Exception {
        int availThreads = Runtime.getRuntime().availableProcessors();
        if (availThreads>4) 
            availThreads -= 2;
        ExecutorService pool = Executors.newFixedThreadPool(availThreads);                     //Set max simultanious threads
        Set<Future<Integer>> set = new HashSet<>();
        //Map<Integer,Future<Integer>> hsMap = new HashMap();
        int sum = 0;
        String[] test = {"this", "is", "a", "test"};
        //while(true) {
            for (String word: test) {
                Callable<Integer> callable = new WordLengthCallable(word);
                Future<Integer> future = pool.submit(callable);
                set.add(future);
                //hsMap.put(sum, future);
            }
        //}
        for (Future<Integer> future : set) {
            sum += future.get();
        }/*
        System.out.printf("The sum of lengths is %s%n", sum);
        */
        pool.shutdown();
        //System.exit(sum);
        
    }
}