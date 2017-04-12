package RCV1;


//For Classification
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import java.util.Map;
import java.util.HashMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.classification.document.KNearestNeighborDocumentClassifier;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.util.BytesRef;


//For Evaluation
import java.io.File;
import java.util.List;
import CommonResources.RanksNL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import com.aliasi.classify.ConfusionMatrix;


//Get list of Training Classes
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.MultiFields;




/**
 *
 * @author sounakbanerjee
 */


public class KNNClassifyRCV1 {
    
    HashMap<String, Object> classifyDoc(KNearestNeighborDocumentClassifier knn, String path) {
        List<ClassificationResult<BytesRef>> pred = null;
        String topics = "";
        HashMap<String, Object> res = new HashMap();
        
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
            
            
            pred = knn.getClasses(luceneDoc);
            res.put("Predicted", pred);
            res.put("Original", topics);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JDOMException ex) {
            Logger.getLogger(KNNClassifyRCV1.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return res;
    }
    
    
    void performClassification(String indexLoc, String testData) {
        try {
            
            //#######Read Index and Train#########
            FSDirectory index = FSDirectory.open(Paths.get(indexLoc));
            IndexReader reader = DirectoryReader.open(index);
            System.out.println("Index : " + reader.getDocCount("Text"));

            //Segmented reader
            List<LeafReaderContext> leaves = reader.leaves();
            System.out.println("Number of leaves: " + leaves.size());
            BM25Similarity BM25 = new BM25Similarity();
            ClassicSimilarity TFIDF = new ClassicSimilarity();
            Map<String, Analyzer> field2analyzer = new HashMap<>();
            field2analyzer.put("Text", new org.apache.lucene.analysis.standard.StandardAnalyzer(RanksNL.stopWords));

            //For single Leaf (Segment in Index)
            LeafReaderContext leaf = leaves.get(0);
            LeafReader atomicReader = leaf.reader();
            System.out.println("leaf : " + atomicReader.getDocCount("Text"));
            KNearestNeighborDocumentClassifier knn = new KNearestNeighborDocumentClassifier(atomicReader,
                    BM25, null, 10, 0, 0, "Topics", field2analyzer, "Text");
            //#######Read Index and Train#########
            
            /*For Multiple Leaves (Segment in Index)
            for (LeafReaderContext leaf : leaves) {
                LeafReader atomicReader = leaf.reader();
                KNearestNeighborDocumentClassifier knn = new KNearestNeighborDocumentClassifier(atomicReader,
                        BM25, null, 10, 0, 0, "Topics", field2analyzer, "text");
            }
             */
            
            
            
            //###########Get Class List of Training Classes##########
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
            //###########Get Class List of Training Classes##########
            
            
            //Create list of Confusion-Matrices
            String[] types = {"true", "false"};
            HashMap<String, ConfusionMatrix> cMatrices = new HashMap();
            ConfusionMatrix cMatrix = null;
            for (String Class : allClassList.split(",")) {
                cMatrix = new ConfusionMatrix(types);
                cMatrices.put(Class, cMatrix);
            }
            
            //########Iterate through Test Docs : Classify##########
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
                        System.out.println("Unknown File: " + file.getAbsolutePath());
                        continue;
                    }
                    HashMap<String, Object> res = classifyDoc(knn, file.getAbsolutePath());
                    //Original Class
                    String originalClasses = (String) res.get("Original");
                    if(originalClasses==null || originalClasses.split(",").length == 0) {
                        System.out.println("No topics for Doc: " + file.getName());
                        continue;
                    }
                    List<String> originalClassList = Arrays.asList(originalClasses.split(","));
                    //Predicted Class
                    List<ClassificationResult<BytesRef>> predResults = (List<ClassificationResult<BytesRef>>) res.get("Predicted");
                    //predResults = predResults.subList(0, originalClassList.size());
                    List<String> predClassList = new ArrayList();
                    for (ClassificationResult<BytesRef> predRes : predResults) {
                        BytesRef resClass = predRes.getAssignedClass();
                        predClassList.add(resClass.utf8ToString());
                    }
                    //System.out.println("File: " + file.getName() + "\n\tOrig: " + originalClasses + "\n\tPred: " + predClassList.toString() + "\n\tScores: ");
                    for (String Class : allClassList.split(",")) {
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
            }
            //########Iterate through Test Docs : Classify##########
            
            //########Report Results##########
            double FSum = 0;
            double tPosSum = 0;
            double fPosSum = 0;
            double fNegSum = 0;
            for (String Class : allClassList.split(",")) {
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
            System.out.println("RCV1.KNNClassifyRCV1");
            double recall = tPosSum/(tPosSum+fNegSum);
            System.out.println("Micro-Rec: " + recall);
            double precision = tPosSum/(tPosSum+fPosSum);
            System.out.println("Micro-Prec: " + precision);
            System.out.println("Micro F-Measure : " + (2*precision*recall)/(precision+recall));
            System.out.println("Avg F-Measure : " + FSum/(double)allClassList.split(",").length);
            //########Report Results##########

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        KNNClassifyRCV1 cl = new KNNClassifyRCV1();
        
        
        //MacOS
        String trainIndex = "/Users/sounakbanerjee/Desktop/Temp/index/RCV1";
        String testData = "/Volumes/Files/Work/Research/Information Retrieval/1) Data/Reuters/RCV1/Manual Subsets/rcv_small/Test";
        
        //Linux
        //String trainIndex = "/home/sounak/work/expesriment Byproducts/index/RCV1";
        //String testData = "/home/sounak/work/Datasets/RCVsubsetTest";
        
        cl.performClassification(trainIndex, testData);
    }
}
