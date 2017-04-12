package RCV1V2;


//For Classification
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import CopulaResources.*;


import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.util.BytesRef;


//For Evaluation
import java.io.File;
import CommonResources.RanksNL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import java.nio.file.Files;
import java.util.Map;



/**
 *
 * @author sounakbanerjee
 */



public class CopulaClassifyRCV1V2 {
    
    
    private HashMap<String, String> createTextMap(String[] text) {
        HashMap<String, String> fileText = new HashMap();
        String currFile = null;
        String Text = "";
        for(String line : text) {
            if (!line.startsWith(".I") && !line.startsWith(".W") && line.length()>1) {
                Text = Text + line.trim() + ". ";
            } else if (line.startsWith(".I")) {
                if (currFile != null && Text != null)
                    fileText.put(currFile, Text);
                currFile = line.split(" ")[1];
                Text = "";
            }
        }
        
        return fileText;
    }
    
    private HashMap<String, List<String>> createTopicsMap(String[] topics) {
        HashMap<String, List<String>> fileTopics = new HashMap();
        String currFile = topics[0].split(" ")[1];
        List<String> topicList = new ArrayList();
        for(String line : topics) {
            if (currFile.equals(line.split(" ")[1])) {
                topicList.add(line.split(" ")[0]);
            } else {
                fileTopics.put(currFile, topicList);
                currFile = line.split(" ")[1];
                topicList = new ArrayList();
                topicList.add(line.split(" ")[0]);
            }
        }
        
        return fileTopics;
    }
    
    private static class classifyDoc implements Callable{
        
        String fileID;
        String fileText;
        //GumbelCopulaClassifierTFIDF classifier;
        GumbelCopulaClassifierTF classifier;
        
        public classifyDoc(GumbelCopulaClassifierTF gcc, String fileID, String fileText) {
            //public classifyDoc(GumbelCopulaClassifierTFIDF gcc, String path) {
            this.fileID = fileID;
            this.fileText = fileText;
            this.classifier = gcc;
        }
        
        @Override
        public HashMap<String, Object> call() {
            
            List<ClassificationResult<BytesRef>> pred = null;
            HashMap<String, Object> res = new HashMap();
            
            /*
            Document luceneDoc = new Document();
            luceneDoc.add(new StringField("DocID", fileID, Field.Store.YES));
            luceneDoc.add(new TextField("Text", fileText, Field.Store.YES));
            */
            try {
                pred = classifier.getClasses(fileText);
                res.put("Predicted", pred);
            } catch (IOException ex) {
                Logger.getLogger(CopulaClassifyRCV1V2.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return res;
        }
    }
    
    
    void performClassification(String indexLoc, String testData, String topics, Path termPairIndex) {
        try {
            
            //#######Read Index and Train#########
            FSDirectory index = FSDirectory.open(Paths.get(indexLoc));
            IndexReader reader = DirectoryReader.open(index);
            Analyzer analyzer = new StandardAnalyzer(RanksNL.stopWords);
            
            
            //Term-Pair
            //TermCooccurence.generateCooccurencebyClass(reader, "Topics", "Text", analyzer, 10, 0, termPairIndex);
            //TermCooccurence.generateNCooccurencebyClass(reader, "Topics", "Text", analyzer, TermCooccurence.TOP, 5, termPairIndex);
            System.out.println("Importing Term Co-occurence");
            //TermCooccurence cooccur = TermCooccurence.generateCooccurencebyClass(reader, "Topics", "Text", analyzer, 2, 10);
            TermCooccurence cooccur = new TermCooccurence(termPairIndex);
            System.out.println("Import Complete");
            
            /*
            //Print all cooccuring terms
            Path tpoutput = Paths.get("/Users/sounakbanerjee/Desktop/Temp/XMLDump");
            cooccur.printtoFile(tpoutput);
            */
            
            //Training the Classifiers
            GumbelCopulaClassifierTF gcc = new GumbelCopulaClassifierTF(reader, analyzer, null, "Topics", cooccur, "Text");
            //GumbelCopulaClassifierTFIDF gcc = new GumbelCopulaClassifierTFIDF(reader, analyzer, null, "Topics", cooccur, "Text");
            //#######Read Index and Train#########
            
            
            
            //###########Get Class List of Training Classes##########
            Terms classes = MultiFields.getTerms(reader, "Topics");
            TermsEnum classesEnum = classes.iterator();
            BytesRef next;
            List<String> allClassList = new ArrayList();
            while ((next = classesEnum.next()) != null) {
                if (next.length > 0) {
                    allClassList.add(next.utf8ToString());
                }
            }
            //System.out.println("Classes : " + allClassList);
            System.out.println("Got Training Classes");
            //###########Get Class List of Training Classes##########
            
            
            //Create list of Confusion-Matrices
            String[] types = {"true", "false"};
            HashMap<String, ConfusionMatrix> cMatrices = new HashMap();
            ConfusionMatrix cMatrix = null;
            for (String Class : allClassList) {
                cMatrix = new ConfusionMatrix(types);
                cMatrices.put(Class, cMatrix);
            }
            
            
            //Read Files
            String[] textAll = Files.readAllLines(new File(testData).toPath()).toArray(new String[0]);
            HashMap<String, String> filesText = createTextMap(textAll);
            System.out.println("---Text Strings Generated---\nSize : " + filesText.size());
            String[] topicsAll = Files.readAllLines(new File(topics).toPath()).toArray(new String[0]);
            HashMap<String, List<String>> filesTopics = createTopicsMap(topicsAll);
            System.out.println("\n---Topic Lists Generated---\nSize : " + filesTopics.size());
            
            //Multicore Processing
            int availThreads = Runtime.getRuntime().availableProcessors();
            if (availThreads>4)
                availThreads -= 2;
            System.out.println("Number of Threads used : " + availThreads);
            ExecutorService pool = Executors.newFixedThreadPool(availThreads);  //Set max simultanious threads
            
            
            //########Iterate through Test Docs : Classify##########
            HashMap<String, Future<HashMap<String, Object>>> hsMap = new HashMap();
            for (Map.Entry<String, String> ent : filesText.entrySet()) {
                String fileID = ent.getKey();
                String fileText = ent.getValue();
                Callable<HashMap<String, Object>> thread = new classifyDoc(gcc, fileID, fileText);
                Future<HashMap<String, Object>> future = pool.submit(thread);
                hsMap.put(fileID, future);
            }
            pool.shutdown();
            
            System.out.println("Number of test-docs: " + hsMap.size());
            for (HashMap.Entry<String, Future<HashMap<String, Object>>> entry : hsMap.entrySet()) {
                Future<HashMap<String, Object>> value = entry.getValue();
                HashMap<String, Object> res = value.get();
                //Original Class
                List<String> originalClassList = filesTopics.get(entry.getKey());
                if(originalClassList == null || originalClassList.isEmpty()) {
                    System.out.println("No topics for Doc: " + entry.getKey());
                    return;
                }
                //Predicted Class
                List<ClassificationResult<BytesRef>> predResults = (List<ClassificationResult<BytesRef>>) res.get("Predicted");
                predResults = predResults.subList(0, originalClassList.size());
                List<String> predClassList = new ArrayList();
                List<Double> predScoreList = new ArrayList();
                for (ClassificationResult<BytesRef> predRes : predResults) {
                    BytesRef resClass = predRes.getAssignedClass();
                    predClassList.add(resClass.utf8ToString());
                    predScoreList.add(predRes.getScore());
                }
                //System.out.println("File: " + entry.getKey() + "\n\tOrig: " + originalClasses + "\n\tPred: " + predClassList.toString() + "\n\tScores: " + predScoreList.toString());
                for (String Class : allClassList) {
                    cMatrix = cMatrices.get(Class);
                    if (originalClassList.contains(Class) && predClassList.contains(Class))
                        cMatrix.increment("true", "true");
                    else if (originalClassList.contains(Class) && !predClassList.contains(Class))
                        cMatrix.increment("true", "false");
                    else if (!originalClassList.contains(Class) && predClassList.contains(Class))
                        cMatrix.increment("false", "true");
                    else if (!originalClassList.contains(Class) && !predClassList.contains(Class))
                        cMatrix.increment("false", "false");
                }
            }
            //########Iterate through Test Docs : Classify##########
            
            //########Report Results##########
            double FSum = 0;
            double tPosSum = 0;
            double fPosSum = 0;
            double fNegSum = 0;
            for (String Class : allClassList) {
                cMatrix = cMatrices.get(Class);
                FSum += Double.isNaN(cMatrix.macroAvgFMeasure()) ? 0 : cMatrix.macroAvgFMeasure();
                tPosSum += cMatrix.oneVsAll(0).truePositive();
                fPosSum += cMatrix.oneVsAll(0).falsePositive();
                fNegSum += cMatrix.oneVsAll(0).falseNegative();
                
                //Print Results:
                //System.out.println("Class : " + Class + "\tMatrix: " + Arrays.deepToString(cMatrix.matrix()));
                //System.out.println("Class : " + Class + "\ttruePositive: " + cMatrix.oneVsAll(0).truePositive());
                //System.out.println("Class : " + Class + "\ttrueNegative: " + cMatrix.oneVsAll(0).trueNegative());
                //System.out.println("Class : " + Class + "\tfalseNegative: " + cMatrix.oneVsAll(0).falseNegative());
                //System.out.println("Class : " + Class + "\tfalsePositive: " + cMatrix.oneVsAll(0).falsePositive());
                //System.out.println("Class : " + Class + "\tMacro-Prec: " + cMatrix.macroAvgPrecision());
                //System.out.println("Class : " + Class + "\tMacro-Rec: " + cMatrix.macroAvgRecall());
                System.out.println("Class : " + Class + "\tMacro-F_Measure: " + cMatrix.macroAvgFMeasure());
            }
            System.out.println("RCV1.CopulaClassifyRCV1V2");
            double recall = tPosSum/(tPosSum+fNegSum);
            System.out.println("Micro-Rec: " + recall);
            double precision = tPosSum/(tPosSum+fPosSum);
            System.out.println("Micro-Prec: " + precision);
            System.out.println("Micro F-Measure : " + (2*precision*recall)/(precision+recall));
            System.out.println("Avg F-Measure : " + FSum/(double)allClassList.size());
            //########Report Results##########
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException | InterruptedException | ExecutionException ex) {
            Logger.getLogger(CopulaClassifyRCV1V2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        CopulaClassifyRCV1V2 cl = new CopulaClassifyRCV1V2();
        
        //MacOS
        String trainIndex = "/Users/sounakbanerjee/Desktop/Temp/index/RCV1V2";
        //String testData = "/Volumes/Files/Work/Research/Information Retrieval/1) Data/Reuters/RCV/RCV/RCV1-V2/Raw Data/TestData/lyrl2004_tokens_test_pt0.dat";
        String testData = "/Users/sounakbanerjee/Desktop/Temp/lyrl2004_tokens_test_pt0.dat";
        String topics = "/Volumes/Files/Work/Research/Information Retrieval/1) Data/Reuters/RCV/RCV/RCV1-V2/Category-File maps/rcv1-v2.topics.qrels";
        Path termPairIndex = Paths.get("/Users/sounakbanerjee/Desktop/Temp/index/RCV1V2/TermPairs");
        
        
        //Linux
        //String trainIndex = "/home/sounak/work/expesriment Byproducts/index/RCV1";
        //String testData = "/home/sounak/work/Datasets/RCVsubsetTest";
        //Path termPairIndex = Paths.get("/home/sounak/work1/test1");
        
        cl.performClassification(trainIndex, testData, topics, termPairIndex);
    }
}