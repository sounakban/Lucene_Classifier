/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classifier;


//Read XML Files
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jdom2.Attribute;
//import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.apache.lucene.document.Document;



//For Indexing
import java.nio.file.Paths;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author sounak
 */
public class Indexer {
    
    //Read Reuters files and return the document data
    void readXML(String path) {
        try {
            File inputFile = new File(path);
            SAXBuilder saxBuilder = new SAXBuilder();
            org.jdom2.Document document = saxBuilder.build(inputFile);
            //System.out.println("Root element :" + document.getRootElement().getName());
            
            Element rootElement = document.getRootElement();
            Attribute IDattribute =  rootElement.getAttribute("itemid");
            
            //System.out.println("DocID : " + IDattribute.getValue() );
            String docID = IDattribute.getValue();
            //System.out.println("Title : " + rootElement.getChild("title").getText());
            String title = rootElement.getChild("title").getText();
            //System.out.println("Headline : "+ rootElement.getChild("headline").getText());
            String headline = rootElement.getChild("headline").getText();
            
            List<Element> docText = rootElement.getChild("text").getChildren();
            //System.out.println("Text : \n");
            String text = "";
            for (int temp = 0; temp < docText.size(); temp++) {
                Element Paragraph = docText.get(temp);
                //System.out.println(Paragraph.getText());
                text = text + Paragraph.getText().trim();
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
                        //System.out.println(": " + allCodes);
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
                        //System.out.println(": " + allCodes);
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
                        //System.out.println(": " + allCodes);
                    }
                }
            }
        }catch(JDOMException e){
            e.printStackTrace();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
    
    void CreateIndex() {
        try{
            //Create a new lucene index;
            String indexDirectoryName = "indexDir";
            File inputDirectory = new File(indexDirectoryName);
            if ( !inputDirectory.exists()){
                inputDirectory.mkdir();
            }
            String corpusFolder= "/home/shanmukh/apps/corpus/";
            Directory dir = FSDirectory.open(Paths.get(indexDirectoryName));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(dir, iwc);
            //Read XML Files
            File folder = new File(corpusFolder);
            File[] listOfFiles = folder.listFiles();
            for ( int j =0 ;  j < listOfFiles.length ; j++) {
                
            }
            writer.close();
            //Print Index Summaries.
            //GetSummary(indexDirectoryName);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
