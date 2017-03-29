package RCV1;


//For Classification
import com.aliasi.classify.ConfusionMatrix;
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
import java.util.ArrayList;
import java.util.Arrays;
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
    
    
    private static class classifyDoc implements Callable{
        
        HashMap<String, Object> res = new HashMap();
        String path;
        GumbelCopulaClassifierTFIDF classifier;
        //GumbelCopulaClassifierTF classifier;
        
        //public classifyDoc(GumbelCopulaClassifierTF gcc, String path) {
        public classifyDoc(GumbelCopulaClassifierTFIDF gcc, String path) {
            this.path = path;
            this.classifier = gcc;
        }
        
        @Override
        public HashMap<String, Object> call() {
            List<ClassificationResult<BytesRef>> pred = null;
            String topics = "";
            
            Document luceneDoc = new Document();
            try {
                
                //###Read current Doc###
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
                
                //###Extract topics###
                List<Element> MetaList = rootElement.getChild("metadata").getChildren();
                for (int i = 0; i < MetaList.size(); i++) {
                    Element Meta = MetaList.get(i);
                    if ("codes".equals(Meta.getName())) {
                        Attribute MetaAttribute = Meta.getAttribute("class");
                        if (MetaAttribute.getValue().contains("topics")) {
                            List<Element> CodeList = Meta.getChildren();
                            for (int j = 0; j < CodeList.size(); j++) {
                                Element Code = CodeList.get(j);
                                Attribute codeAttribute = Code.getAttribute("code");
                                topics = topics + codeAttribute.getValue();
                                if (j != CodeList.size() - 1) {
                                    topics = topics + ",";
                                }
                            }
                        }
                    }
                }
                //###Extract topics###
                
                
                pred = classifier.getClasses(text);
                res.put("Predicted", pred);
                if(topics==null || topics.equals(""))
                    res.put("Original", null);
                res.put("Original", topics);
                
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JDOMException ex) {
                Logger.getLogger(CopulaClassifyRCV1.class.getName()).log(Level.SEVERE, null, ex);
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
            
            
            //GumbelCopulaClassifierTF gcc = new GumbelCopulaClassifierTF(reader, analyzer, null, "Topics", cooccur, "Text");
            GumbelCopulaClassifierTFIDF gcc = new GumbelCopulaClassifierTFIDF(reader, analyzer, null, "Topics", cooccur, "Text");
            //#######Read Index and Train#########
            
            
            
            //###########Get Class List of Training Classes##########
            System.out.println("Getting Training Classes");
            Terms classes = MultiFields.getTerms(reader, "Topics");
            TermsEnum classesEnum = classes.iterator();
            BytesRef next;
            String allClassList = "";
            while ((next = classesEnum.next()) != null) {
                if (next.length > 0) {
                    allClassList = allClassList + next.utf8ToString() + ",";
                }
            }
            //System.out.println("Classes : " + allClassList);=
            System.out.println("Got Training Classes");
            //###########Get Class List of Training Classes##########
            
            
            //Create list of Confusion-Matrices
            String[] types = {"true", "false"};
            HashMap<String, ConfusionMatrix> cMatrices = new HashMap();
            ConfusionMatrix cMatrix = null;
            for (String Class : allClassList.split(",")) {
                cMatrix = new ConfusionMatrix(types);
                cMatrices.put(Class, cMatrix);
            }

            
            //Multicore Processing
            int availThreads = Runtime.getRuntime().availableProcessors();
            if (availThreads>4)
                availThreads -= 2;
            System.out.println("Number of Threads used : " + availThreads);
            ExecutorService pool = Executors.newFixedThreadPool(availThreads);  //Set max simultanious threads
            
            
            //########Iterate through Test Docs : Classify##########                    
            HashMap<String, Future<HashMap<String, Object>>> hsMap = new HashMap();
            File testFolder = new File(testData);
            File[] listOfFolders = testFolder.listFiles();
            if (listOfFolders == null){
                System.out.println("Empty directory!!!");
                return;
            }
            for (File subFolder : listOfFolders) {
                File currFolder = new File(subFolder.getAbsolutePath());
                File[] listOfFiles = currFolder.listFiles();
                if (listOfFiles == null){
                    System.out.println("Empty directory: " + subFolder.getAbsolutePath());
                    continue;
                }
                for (File file : listOfFiles) {
                    if (!(file.getName().contains(".xml") || file.getName().contains(".XML"))) {
                        System.out.println("Unknown File: " + file.getName());
                        //System.out.println("Unknown File: " + file.getAbsolutePath());
                        continue;
                    }
                    Callable<HashMap<String, Object>> thread = new classifyDoc(gcc, file.getAbsolutePath());
                    Future<HashMap<String, Object>> future = pool.submit(thread);
                    hsMap.put(file.getAbsolutePath(), future);
                }
            }
            pool.shutdown();

            System.out.println("Number of test-docs: " + hsMap.size());
            for (HashMap.Entry<String, Future<HashMap<String, Object>>> entry : hsMap.entrySet()) {
                Future<HashMap<String, Object>> value = entry.getValue();
                HashMap<String, Object> res = value.get();
                //Original Class
                String originalClasses = (String) res.get("Original");
                List<String> originalClassList = Arrays.asList(originalClasses.split(","));
                if(originalClasses==null) {
                    System.out.println("No topics for Doc: " + entry.getKey());
                    continue;
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
                System.out.println("File: " + entry.getKey() + "\n\tOrig: " + originalClasses + "\n\tPred: " + predClassList.toString() + "\n\tScores: " + predScoreList.toString());
                for (String Class : allClassList.split(",")) {
                    cMatrix = cMatrices.get(Class);
                    if (originalClassList.contains(Class) && predClassList.contains(Class)) {
                        //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                        cMatrix.increment("true", "true");
                    }
                    else if (originalClassList.contains(Class) && !predClassList.contains(Class)) {
                        //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                        cMatrix.increment("true", "false");
                    }
                    else if (!originalClassList.contains(Class) && predClassList.contains(Class)) {
                        //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                        cMatrix.increment("false", "true");
                    }
                    else if (!originalClassList.contains(Class) && !predClassList.contains(Class)) {
                        //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                        cMatrix.increment("false", "false");
                    }
                }
            }
            //########Iterate through Test Docs : Classify##########
            
            //########Report Results##########
            double FSum = 0;
            for (String Class : allClassList.split(",")) {
                cMatrix = cMatrices.get(Class);
                System.out.println("Class : " + Class + "\tF-Measure: " + cMatrix.microAverage().fMeasure());
                FSum += cMatrix.microAverage().fMeasure();
                //System.out.println("Precision: " + cMatrix.getAvgPrecision());
                //System.out.println("Recall: " + cMatrix.getAvgRecall());
                
            }
            System.out.println("Macro F-Measure : " + FSum/(double)allClassList.split(",").length);
            //########Report Results##########

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException | InterruptedException | ExecutionException ex) {
            Logger.getLogger(CopulaClassifyRCV1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        CopulaClassifyRCV1 cl = new CopulaClassifyRCV1();
        
        //MacOS
        String trainIndex = "/Users/sounakbanerjee/Desktop/Temp/index/RCV1";
        //String testData = "/Volumes/Files/Work/Research/Information Retrieval/Data/Reuters/RCV1/Manual Subsets/rcv_small/Test";
        String testData = "/Volumes/Files/Work/Research/Information Retrieval/Data/Reuters/RCV1/Manual Subsets/rcv_small/verysmall";
        Path termPairIndex = Paths.get("/Users/sounakbanerjee/Desktop/Temp/index/RCV1/TermPairs");
        
        //Linux
        //String trainIndex = "/home/sounak/work/expesriment Byproducts/index/RCV1";
        //String testData = "/home/sounak/work/Datasets/RCVsubsetTest";
        //Path termPairIndex = Paths.get("/home/sounak/work1/test1");
        
        cl.performClassification(trainIndex, testData, termPairIndex);
    }
}