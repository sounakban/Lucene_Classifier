package Reuters21578;


//For Classification
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
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
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import com.aliasi.classify.ConfusionMatrix;



/**
 *
 * @author sounakbanerjee
 */



public class CopulaClassifyR21578 {
    
    private static class classifyDoc implements Callable{
        
        private ClassificationResult<BytesRef> res=null;
        String path;
        //GumbelCopulaClassifierTFIDF classifier;
        GumbelCopulaClassifierTF classifier;

        public classifyDoc(GumbelCopulaClassifierTF gcc, String path) {
        //public classifyDoc(GumbelCopulaClassifierTFIDF gcc, String path) {
            this.path = path;
            this.classifier = gcc;
        }
        
        @Override
        public ClassificationResult<BytesRef> call() {
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
                
                res = classifier.assignClass(text);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            return res;
        }
    }
    
    
    
    void performClassification(String indexLoc, String testData, Path termPairIndex) {
        try {
            
            //#######Read Index and Train#########
            FSDirectory index = FSDirectory.open(Paths.get(indexLoc));
            IndexReader reader = DirectoryReader.open(index);
            Analyzer analyzer = new StandardAnalyzer(RanksNL.stopWords);
            
            
            //Term-Pair
            //TermCooccurence.generateCooccurencebyClass(reader, "Topics", "Text", analyzer, 0, 0, termPairIndex);
            System.out.println("Importing Term Pairs");
            //TermCooccurence cooccur = TermCooccurence.generateCooccurencebyClass(reader, "Topics", "Text", analyzer, 0, 0);
            TermCooccurence cooccur = new TermCooccurence(termPairIndex);
            System.out.println("Import Complete");
            
            
            GumbelCopulaClassifierTF gcc = new GumbelCopulaClassifierTF(reader, analyzer, null, "Topics", cooccur, "Text");
            //GumbelCopulaClassifierTFIDF gcc = new GumbelCopulaClassifierTFIDF(reader, analyzer, null, "Topics", cooccur, "Text");
            //#######Read Index and Train#########
            
           //###########Get Class List of Training Docs##########
            Terms classes = MultiFields.getTerms(reader, "Topics");
            TermsEnum classesEnum = classes.iterator();
            BytesRef next;
            String allClassList = "";
            while ((next = classesEnum.next()) != null) {
                if (next.length > 0) {
                    allClassList = allClassList + next.utf8ToString() + ",";
                }
            }
            //System.out.println("Classes : " + allClassList);
            //###########Get Class List of Training Docs##########
            
            
            ConfusionMatrix cMatrix = new ConfusionMatrix(allClassList.split(","));
            
            //Multicore Processing
            int availThreads = Runtime.getRuntime().availableProcessors();
            if (availThreads>4)
                availThreads -= 2;
            System.out.println("Number of Threads used : " + availThreads);
            ExecutorService pool = Executors.newFixedThreadPool(availThreads);  //Set max simultanious threads
            
            //########Iterate through Test Docs : Classify & Report##########                    
            HashMap<String, Future<ClassificationResult<BytesRef>>> hsMap = new HashMap();
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
                    Callable<ClassificationResult<BytesRef>> thread = new classifyDoc(gcc, file.getAbsolutePath());
                    Future<ClassificationResult<BytesRef>> future = pool.submit(thread);
                    hsMap.put(file+":"+originalClass, future);
                }
            }
            pool.shutdown();

            System.out.println("Number of test-docs: " + hsMap.size());
            for (HashMap.Entry<String, Future<ClassificationResult<BytesRef>>> entry : hsMap.entrySet())
            {
                String originalClass = entry.getKey().split(":")[1];
                Future<ClassificationResult<BytesRef>> value = entry.getValue();
                ClassificationResult<BytesRef> res = value.get();
                BytesRef resClass = res.getAssignedClass();
                String predClass = resClass.utf8ToString();
                //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                cMatrix.increment(originalClass, predClass);
            }
            System.out.println("Reuters21578.CopulaClassifyR215781");
            System.out.println("Micro F-Measure: " + cMatrix.microAverage().fMeasure());
            System.out.println("Macro F-Measure: " + cMatrix.macroAvgFMeasure());
            //System.out.println("Precision: " + cMatrix.);
            //System.out.println("Recall: " + cMatrix.);
            
            //########Iterate through Test Docs : Classify & Report##########
            
            /*
            for (Map.Entry<String, Double> fMes : cMatrix.getFMeasureForLabels().entrySet()) {
                System.out.println("Class: " + fMes.getKey() + "\tF-Measure: " + fMes.getValue());
            }
            */
            
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException | InterruptedException | ExecutionException ex) {
            Logger.getLogger(CopulaClassifyR21578.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        CopulaClassifyR21578 cl = new CopulaClassifyR21578();
        
        //Mac Paths
        String trainIndex = "/Users/sounakbanerjee/Desktop/Temp/index/R21578";
        String testData = "/Volumes/Files/Work/Research/Information Retrieval/1) Data/Reuters/Reuters21578-Apte-top10/Reuters21578-Apte-top10/test";
        Path termPairIndex = Paths.get("/Users/sounakbanerjee/Desktop/Temp/index/R21578/TermPairs");
        
        
        //Linux Paths
        //String testData = "/home/sounak/work/Datasets/Reuters21578-Apte-top10/test";
        //String indexLoc = "/home/sounak/work/expesriment Byproducts/index/reuters21578";
        //Path termPairIndex = Paths.get("/home/sounak/work1/test1");
        
        cl.performClassification(trainIndex, testData, termPairIndex);
    }
}