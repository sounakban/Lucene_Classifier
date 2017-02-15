/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TesterClasses;


//Read XML Files
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;


//Read Text
import java.io.FileReader;
import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Paths;

//Split to sentences
import java.text.BreakIterator;
import java.util.Locale;


//Test TermPair Class
import java.util.HashMap;
import CopulaResources.TermPair;
import CopulaResources.TermCooccurence;
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;


/**
 *
 * @author sounakbanerjee
 */
public class test {
    
    static<T> void println(T arg) { System.out.println(arg); }
    
    static HashMap<TermPair, Double> CooccurenceList;
    
    
    
    
    static void readXML(String path) {
        try {
         File inputFile = new File(path);

         SAXBuilder saxBuilder = new SAXBuilder();

         Document document = saxBuilder.build(inputFile);

         System.out.println("Root element :" 
            + document.getRootElement().getName());

         Element rootElement = document.getRootElement();
         Attribute IDattribute =  rootElement.getAttribute("itemid");
         System.out.println("DocID : " + IDattribute.getValue() );
         
         System.out.println("Title : " + rootElement.getChild("title").getText());
         
         System.out.println("Headline : "+ rootElement.getChild("headline").getText());
         
         List<Element> docText = rootElement.getChild("text").getChildren();
         System.out.println("Text : \n");
         for (int temp = 0; temp < docText.size(); temp++) {  
            Element Paragraph = docText.get(temp); 
            System.out.println(Paragraph.getText());
         }
         
         List<Element> MetaList = rootElement.getChild("metadata").getChildren();
         for (int i = 0; i < MetaList.size(); i++) {
             Element Meta = MetaList.get(i);
             if ("codes".equals(Meta.getName())) {
                 Attribute MetaAttribute =  Meta.getAttribute("class");
                 String allCodes = "";
                 if(MetaAttribute.getValue().contains("topics")) {
                     List<Element> CodeList = Meta.getChildren();
                     for (int j = 0; j < CodeList.size(); j++) {  
                         Element Code = CodeList.get(j);
                         Attribute codeAttribute =  Code.getAttribute("code");
                         allCodes = allCodes + codeAttribute.getValue();
                            if (j != CodeList.size()-1)
                                allCodes = allCodes + ",";
                     }
                     System.out.println(": " + allCodes);
                 }
                 else if(MetaAttribute.getValue().contains("countries")) {
                     List<Element> CodeList = Meta.getChildren();
                     for (int j = 0; j < CodeList.size(); j++) {  
                         Element Code = CodeList.get(j);
                         Attribute codeAttribute =  Code.getAttribute("code");
                         allCodes = allCodes + codeAttribute.getValue();
                            if (j != CodeList.size()-1)
                                allCodes = allCodes + ",";
                     }
                     System.out.println(": " + allCodes);
                 }
                 else if(MetaAttribute.getValue().contains("industries")) {
                     List<Element> CodeList = Meta.getChildren();
                     for (int j = 0; j < CodeList.size(); j++) {  
                         Element Code = CodeList.get(j);
                         Attribute codeAttribute =  Code.getAttribute("code");
                         allCodes = allCodes + codeAttribute.getValue();
                            if (j != CodeList.size()-1)
                                allCodes = allCodes + ",";
                     }
                     System.out.println(": " + allCodes);
                 }
             }
             
         }
         
      }catch(JDOMException | IOException e){
         e.printStackTrace();
      }
    }
    
    
    static void readText(String path) {
        try {
            File inputFile = new File(path);
            FileReader inputFileReader = new FileReader(inputFile);
            BufferedReader read = new BufferedReader(inputFileReader);
            
            String line = "";
            while (line.equals(""))    line = read.readLine();
            System.out.println("Headline : " + line.replaceAll("\\<.*?\\> ", "").trim());
            
            StringBuffer text = new StringBuffer();
            while ((line = read.readLine()) != null) {
                text.append(line.trim() + " ");
            }
            System.out.println("Text : " + text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main(String[] args) throws IOException {
        
        //Variables for common use
        Integer count;
        StringBuilder sb;
        String source;
        int start;
        Double val;
        Path flpth = Paths.get("/home/sounak/work1/test1");
        
        // ###########Find variable types and content#########
        //println("\n"+"###########Find variable types and content#########");
        //println("Field_ID : type : "+FIELD_ID.getClass().getName()+"     Content : "+FIELD_ID);
        //println("Field_BOW : type : "+FIELD_BOW.getClass().getName()+"     Content : "+FIELD_BOW);
        
        // ###########String Builder Experiments#########
        println("\n"+"###########String Builder Experiments#########");
        sb = new StringBuilder("");
        sb.append("test");
        println("Test to see StrBld: "+sb);
        sb.deleteCharAt(sb.length()-1);
        println("After deleteCharAt: "+sb);
        
        
        //Read XML Files
        //readXML("/Users/sounakbanerjee/Desktop/Temp/untitled folder/780713newsML.xml");
        
        
        //Read Simple text
        //readText("/Volumes/Files/Current/Problems/4) ISI/Practical/Datasets/Reuters21578-Apte-top10/training/acq/0000005");
        
        
        //Split sentences
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.US);
        source = "This is a test. This is a T.L.A. test, Now with a Dr. in it.";
        sentences.setText(source);
        start = sentences.first();  count = 1;
        for (int end = sentences.next(); end != BreakIterator.DONE; start = end, end = sentences.next()) {
            System.out.println(count + " : " + source.substring(start,end));
            count++;
        }
        
        
        //Create term-Pairs
        BreakIterator wordlist = BreakIterator.getWordInstance();
        start = sentences.first();
        for (int end = sentences.next(); end != BreakIterator.DONE; start = end, end = sentences.next()) {
            String sentence = source.substring(start,end); 
            wordlist.setText(sentence);
            ArrayList<String> words = new ArrayList();
            count = 0;
            int wordstart = wordlist.first();
            for (int wordend = wordlist.next(); wordend != BreakIterator.DONE; wordstart = wordend, wordend = wordlist.next()) {
                if (sentence.substring(wordstart,wordend).length() > 1) {
                    words.add(sentence.substring(wordstart,wordend));
                }
            }
            for (int i = 0 ; i < words.size() ; i++) {
                for (int j = i+1 ; j < words.size() ; j++) {
                    System.out.println(count + " : " + words.get(i) + " , " + words.get(j));
                    count++;
                }
            }
        }
        
        
        
        //Test Termpair Class
        test.CooccurenceList = new HashMap();
        TermPair t1 = new TermPair("Just a Coincedence","correlation");
        TermPair t2 = new TermPair("correlation","Just a Coincedence");
        System.out.println("Are they equal? : " + t1.equals(t2));
        System.out.println("First HashCode : " + t1.hashCode());
        System.out.println("Second HashCode : " + t2.hashCode());
        val = 42.0;
        CooccurenceList.put(t1, val);
        System.out.println("Keyvalue found? : " + CooccurenceList.get(t2));
        
        
        
        //Test term Co-Occurence Class
        TermPair t3 = new TermPair("correlation","Not a Coincedence");
        TermCooccurence tc1 = new TermCooccurence();
        tc1.increaseCount(t1, "");
        count = tc1.getCount(t1, "");
        System.out.println("Count of t1 : " + count);
        count = tc1.getCount(t2, "");
        System.out.println("Count of t2 : " + count);
        count = tc1.getCount(t3, "");
        Boolean isnull = (count==null);
        System.out.println("Count of t3 : " + isnull);
        tc1.increaseCountbyn(t2, "", 7);
        count = tc1.getCount(t2, "");
        System.out.println("Count of t2 : " + count);
        tc1.increaseCount(t1, "");
        count = tc1.getCount(t1, "");
        System.out.println("Count of t1 : " + count);
        if (tc1.getCount(t3, "") == null) {
            System.out.println("Pair not  found");
        }
        tc1.savetoFile(flpth);
        try {
            TermCooccurence tc2 = new TermCooccurence(flpth);
            count = tc2.getCount(t2, "");
            System.out.println("Count of t2 : " + count);
            count = tc2.getCount(t1, "");
            System.out.println("Count of t1 : " + count);
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }
        
        
        //Create new file at Path
        File dir = new File("/home/sounak/work1/test1");
        dir.mkdirs();
        File file = new File(dir, "filename.txt");
        file.createNewFile();
        
        //temporary
        FSDirectory index = FSDirectory.open(Paths.get("/home/sounak/work/Datasets/index/reuters21578"));
        IndexReader reader = DirectoryReader.open(index);
        Analyzer analyzer = new StandardAnalyzer();
        TermCooccurence.generateCooccurencebyClass(reader, "Topics", "Text", analyzer, 2, 10, flpth);
        
        
    }
}
