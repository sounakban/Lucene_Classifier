/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Others;

/**
 *
 * @author sounak
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

/**
 *
 * @author Niraj
 */
public final class TestAnalyzer {

    public static List tokenizeString(Analyzer analyzer, String str) {
        List result = new ArrayList<>();
        try {
            TokenStream stream  = analyzer.tokenStream(null, new StringReader(str));
            stream.reset();
            while (stream.incrementToken()) {
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
        } catch (IOException e) {
            // not thrown b/c we're using a string reader...
            throw new RuntimeException(e);
        }
        return result;
    }
  
  public static void main(String[] args) {
      String text = "Lucene is a simple simpler yet powerful java based search library. I am try trying to make it work.";
      Analyzer analyzer = new StandardAnalyzer();
      //Analyzer analyzer = new EnglishAnalyzer();
      List ss = TestAnalyzer.tokenizeString(analyzer, text);
      System.out.print("==>"+ss+" \n");
  }
}