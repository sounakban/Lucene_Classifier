package RCV1;


//For Classification
import CommonResources.ConfusionMatrix;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import CopulaResources.TermCooccurence;
import CopulaResources.*;


import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.util.BytesRef;


//For Evaluation
import java.io.File;
import CommonResources.RanksNL;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;



//For Reading test Docs
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;



/**
 *
 * @author sounakbanerjee
 */



public class CopulaClassifyRCV1 {

    //ClassificationResult<BytesRef> classifyDoc(GumbelCopulaClassifierTF gcc, String path) {
    ClassificationResult<BytesRef> classifyDoc(GumbelCopulaClassifierTFIDF gcc, String path) {

        ClassificationResult<BytesRef> res=null;
        try {
            
            //###Read current Doc###
            Document luceneDoc = new Document();
            File inputFile = new File(path);
            SAXBuilder saxBuilder = new SAXBuilder();
            org.jdom2.Document document = saxBuilder.build(inputFile);

            Element rootElement = document.getRootElement();
            Attribute IDattribute = rootElement.getAttribute("itemid");
            String docID = IDattribute.getValue();
            //System.out.println("DocID : " + docID );
            luceneDoc.add(new StringField("DocID", docID, Field.Store.YES));

            //System.out.println("Headline : "+ rootElement.getChild("headline").getText());
            String headline = rootElement.getChild("headline").getText();
            luceneDoc.add(new StringField("Headline", headline, Field.Store.YES));

            List<Element> docText = rootElement.getChild("text").getChildren();
            //System.out.println("Text : \n");
            String text = "";
            for (int temp = 0; temp < docText.size(); temp++) {
                Element Paragraph = docText.get(temp);
                //System.out.println(Paragraph.getText());
                text = text + Paragraph.getText().trim();
            }
            luceneDoc.add(new TextField("Text", text, Field.Store.YES));

            //###Read current Doc###

            res = gcc.assignClass(text);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JDOMException ex) {
            Logger.getLogger(CopulaClassifyRCV1.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return res;
    }
    
    
    void performClassification(String indexLoc, String testData) {
        try {
            
            //#######Read Index and Train#########
            FSDirectory index = FSDirectory.open(Paths.get(indexLoc));
            IndexReader reader = DirectoryReader.open(index);
            Analyzer analyzer = new StandardAnalyzer(RanksNL.stopWords);
            
            
            //Term-Pair
            //TermCooccurence cooccur = TermCooccurence.generateCooccurencebyClass(reader, "Topics", "Text", analyzer, 2, 10);
            Path termPairIndex = Paths.get("/home/sounak/work1/test1");
            //TermCooccurence.generateCooccurencebyClass(reader, "Topics", "Text", analyzer, 2, 10, termPairIndex);
            TermCooccurence cooccur = new TermCooccurence(termPairIndex);
            

            //BM25Similarity BM25 = new BM25Similarity();
            //Map<String, Analyzer> field2analyzer = new HashMap<>();
            //field2analyzer.put("Text", new org.apache.lucene.analysis.standard.StandardAnalyzer(RanksNL.stopWords));
            
            //GumbelCopulaClassifierTF gcc = new GumbelCopulaClassifierTF(reader, analyzer, null, "Topics", cooccur, "Text");
            GumbelCopulaClassifierTFIDF gcc = new GumbelCopulaClassifierTFIDF(reader, analyzer, null, "Topics", cooccur, "Text");
            //#######Read Index and Train#########
            
            
            ConfusionMatrix cMatrix = new ConfusionMatrix();
            
            //########Iterate through Test Docs : Classify & Report##########
            File testFolder = new File(testData);
            File[] listOfFolders = testFolder.listFiles();
            for (File folder : listOfFolders) {
                File classFolder = new File(folder.getAbsolutePath());
                String originalClass = classFolder.getName();
                File[] listOfFiles = classFolder.listFiles();
                for (File file : listOfFiles) {
                    if (file.getName().contains("\\.")) {
                        System.out.println("Unknown File: " + file.getAbsolutePath());
                        continue;
                    }
                    ClassificationResult<BytesRef> res = classifyDoc(gcc, file.getAbsolutePath());
                    BytesRef resClass = res.getAssignedClass();
                    String predClass = resClass.utf8ToString();
                    //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                    cMatrix.increaseValue(originalClass, predClass);
                }
            }
            System.out.println("F-Measure: " + cMatrix.getMacroFMeasure());
            System.out.println("Precision: " + cMatrix.getAvgPrecision());
            System.out.println("Recall: " + cMatrix.getAvgRecall());
            
            //########Iterate through Test Docs : Classify & Report##########

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(CopulaClassifyRCV1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        CopulaClassifyRCV1 cl = new CopulaClassifyRCV1();
        //testData = "/Volumes/Files/Current/Drive/Work/Experiment/Reuters21578-Apte-top10/training";
        //String indexLoc = "/Users/sounakbanerjee/Desktop/Temp/index";
        String testData = "/home/sounak/work/Datasets/Reuters21578-Apte-top10/test";
        String indexLoc = "/home/sounak/work/expesriment Byproducts/index/reuters21578";
        cl.performClassification(indexLoc, testData);
    }
}