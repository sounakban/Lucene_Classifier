/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package TesterClasses;

import java.nio.file.Path;
import java.nio.file.Paths;
import CopulaResources.TermCooccurence;

/**
 *
 * @author sounakbanerjee
 */
public class TestcOccurValues {
    public static void writeVocabulary(TermCooccurence cooccur) {
        
        String[] classes = cooccur.getClassList();
        try
        {
            for(String Class : classes) {
                cooccur.
            }
            // Creating the writer
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
            
            for (int i = 1; i < listVal.size(); i++) {
                out.println(listVal.get(i) + " " + vocab.get(i));
            }
            
            out.close();
        }
        // Catching the file not found error
        // and any other errors
        catch (FileNotFoundException e) {
            System.err.println(fileName + "cannot be found.");
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }
    
    public static void main(String[] args) {
        Path termPairIndex = Paths.get("/Users/sounakbanerjee/Desktop/Temp/index/RCV1/TermPairs");
        TermCooccurence cooccur = new TermCooccurence(termPairIndex);
        writeVocabulary(cooccur);
    }
}
