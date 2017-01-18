/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classifier;

//Variable types and content
import static common.CommonVariables.FIELD_BOW;
import static common.CommonVariables.FIELD_ID;

//Read XML Files
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author sounakbanerjee
 */
public class test {
    
    static<T> void println(T arg) { System.out.println(arg); }
    
    
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
             if (Meta.getName() == "codes") {
                 Attribute MetaAttribute =  Meta.getAttribute("class");
                 String allCodes = "";
                 if(MetaAttribute.getValue().contains("topics")) {
                     List<Element> CodeList = Meta.getChildren();
                     for (int j = 0; j < CodeList.size(); j++) {  
                         Element Code = CodeList.get(j);
                         Attribute codeAttribute =  Code.getAttribute("code");
                         allCodes = allCodes + codeAttribute.getValue();
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
                         allCodes = allCodes + ",";
                     }
                     System.out.println(": " + allCodes);
                 }
             }
             
         }
         
      }catch(JDOMException e){
         e.printStackTrace();
      }catch(IOException ioe){
         ioe.printStackTrace();
      }
    }
    
    
    public static void main(String[] args) {
        
        // ###########Find variable types and content#########
        println("\n"+"###########Find variable types and content#########");
        println("Field_ID : type : "+FIELD_ID.getClass().getName()+"     Content : "+FIELD_ID);
        println("Field_BOW : type : "+FIELD_BOW.getClass().getName()+"     Content : "+FIELD_BOW);
        
        // ###########String Builder Experiments#########
        println("\n"+"###########String Builder Experiments#########");
        StringBuilder sb = new StringBuilder("");
        sb.append("test");
        println("Test to see StrBld: "+sb);
        sb.deleteCharAt(sb.length()-1);
        println("After deleteCharAt: "+sb);
        
        
        //Read XML Files
        readXML("/home/sounak/work/2286newsML.xml");
    }
}
