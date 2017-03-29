package Extra;


//For Classification
import CommonResources.ConfusionMatrix;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import CopulaResources.*;
import CopulaResources.TermCooccurence;


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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;



/**
 *
 * @author sounakbanerjee
 */



public class CopulaClassifyR21578_SingleCore {

    //ClassificationResult<BytesRef> classifyDoc(GumbelCopulaClassifierTF gcc, String path) {
    ClassificationResult<BytesRef> classifyDoc(GumbelCopulaClassifierTFIDF gcc, String path) {

        ClassificationResult<BytesRef> res=null;
        try {
            
            //###Read current Doc###
            Document luceneDoc = new Document();
            File inputFile = new File(path);
            FileReader inputFileReader = new FileReader(inputFile);
            BufferedReader read = new BufferedReader(inputFileReader);

            //System.out.println("DocID : " + inputFile.getName() );
            String docID = inputFile.getName();
            luceneDoc.add(new StringField("DocID", docID, Field.Store.YES));

            String line = "";
            while (line.equals("")) {
                line = read.readLine();
            }
            //System.out.println("Headline : "+ line.replaceAll("\\<.*?\\> ", "").trim());
            String headline = line.replaceAll("\\<.*?\\> ", "").trim();
            luceneDoc.add(new StringField("Headline", headline, Field.Store.YES));

            StringBuffer temp = new StringBuffer();
            while ((line = read.readLine()) != null) {
                temp.append(line.trim()).append(" ");
            }
            String text = temp.toString();
            //System.out.println("Text : \n" + text);
            luceneDoc.add(new TextField("Text", text, Field.Store.YES));

            //###Read current Doc###

            res = gcc.assignClass(text);

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return res;
    }
    
    
    void performClassification(String indexLoc, String testData, Path termPairIndex) {
        try {
            
            //#######Read Index and Train#########
            FSDirectory index = FSDirectory.open(Paths.get(indexLoc));
            IndexReader reader = DirectoryReader.open(index);
            Analyzer analyzer = new StandardAnalyzer(RanksNL.stopWords);
            
            
            //Term-Pair
            //TermCooccurence cooccur = TermCooccurence.generateCooccurencebyClass(reader, "Topics", "Text", analyzer, 2, 10);
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
            Logger.getLogger(CopulaClassifyR21578_SingleCore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        CopulaClassifyR21578_SingleCore cl = new CopulaClassifyR21578_SingleCore();
        
        //MacOS
        //String trainIndex = "/Users/sounakbanerjee/Desktop/Temp/index/RCV1";
        //String testData = "/Volumes/Files/Current/Drive/Work/Experiment/RCV1/Test";
        //Path termPairIndex = Paths.get("/Users/sounakbanerjee/Desktop/Temp/index/RCV1/TermPairs");
        
        //Linux
        String testData = "/home/sounak/work/Datasets/Reuters21578-Apte-top10/test";
        String indexLoc = "/home/sounak/work/expesriment Byproducts/index/reuters21578";
        Path termPairIndex = Paths.get("/home/sounak/work1/test1");
        
        cl.performClassification(indexLoc, testData, termPairIndex);
    }
}