package RCV1;


//For Classification
import CommonResources.ConfusionMatrix;
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
import org.apache.lucene.classification.document.SimpleNaiveBayesDocumentClassifier;
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
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/*
//Get list of Training Classes
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MultiFields;
*/



/**
 *
 * @author sounakbanerjee
 */


public class NaiveBayesClassifyRCV1 {

    HashMap<String, Object> classifyDoc(SimpleNaiveBayesDocumentClassifier snb, String path) {
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
            
            
            pred = snb.getClasses(luceneDoc);
            res.put("Predicted", pred);
            res.put("Original", topics);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JDOMException ex) {
            Logger.getLogger(NaiveBayesClassifyRCV1.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return res;
    }
    
    
    void performClassification(String indexLoc, String testData) {
        try {
            
            //#######Read Index and Train#########
            FSDirectory index = FSDirectory.open(Paths.get(indexLoc));
            IndexReader reader = DirectoryReader.open(index);

            //Segmented reader
            List<LeafReaderContext> leaves = reader.leaves();
            System.out.println("Number of leaves: " + leaves.size());
            BM25Similarity BM25 = new BM25Similarity();
            ClassicSimilarity TFIDF = new ClassicSimilarity();
            Map<String, Analyzer> field2analyzer = new HashMap<>();
            field2analyzer.put("Text", new org.apache.lucene.analysis.standard.StandardAnalyzer(RanksNL.stopWords));
            
            /*For Multiple Leaves (Segment in Index)
            for (LeafReaderContext leaf : leaves) {
                LeafReader atomicReader = leaf.reader();
                KNearestNeighborDocumentClassifier knn = new KNearestNeighborDocumentClassifier(atomicReader,
                        BM25, null, 10, 0, 0, "Topics", field2analyzer, "text");
            }
             */

            //For single Leaf (Segment in Index)
            LeafReaderContext leaf = leaves.get(0);
            LeafReader atomicReader = leaf.reader();
            SimpleNaiveBayesDocumentClassifier snb = new SimpleNaiveBayesDocumentClassifier(atomicReader,
                    null, "Topics", field2analyzer, "Text");
            //#######Read Index and Train#########
            
            
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
            //System.out.println("Classes : " + allClassList);=
            //###########Get Class List of Training Classes##########
            
            
            //Create list of Confusion-Matrices
            HashMap<String, ConfusionMatrix> cMatrices = new HashMap();
            ConfusionMatrix cMatrix = null;
            for (String Class : allClassList.split(",")) {
                cMatrix = new ConfusionMatrix();
                cMatrices.put(Class, cMatrix);
            }
            
            //########Iterate through Test Docs : Classify##########
            File testFolder = new File(testData);
            File[] listOfFolders = testFolder.listFiles();
            for (File folder : listOfFolders) {
                File classFolder = new File(folder.getAbsolutePath());
                File[] listOfFiles = classFolder.listFiles();
                for (File file : listOfFiles) {
                    if (file.getName().contains("\\.")) {
                        System.out.println("Unknown File: " + file.getAbsolutePath());
                        continue;
                    }
                    HashMap<String, Object> res = classifyDoc(snb, file.getAbsolutePath());
                    //Predicted Class
                    List<ClassificationResult<BytesRef>> predResults = (List<ClassificationResult<BytesRef>>) res.get("Predicted");
                    List<String> predClassList = new ArrayList();
                    for (ClassificationResult<BytesRef> predRes : predResults) {
                        BytesRef resClass = predRes.getAssignedClass();
                        predClassList.add(resClass.utf8ToString());
                    }
                    //Original Class
                    String originalClasses = (String) res.get("Original");
                    List<String> originalClassList = Arrays.asList(originalClasses.split(","));
                    for (String Class : allClassList.split(",")) {
                        cMatrix = cMatrices.get(Class);
                        if (originalClassList.contains(Class) && predClassList.contains(Class)) {
                            //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                            cMatrix.increaseValue("true", "true");
                        }
                        else if (!originalClassList.contains(Class) && predClassList.contains(Class)) {
                            //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                            cMatrix.increaseValue("false", "true");
                        }
                        else if (originalClassList.contains(Class) && !predClassList.contains(Class)) {
                            //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                            cMatrix.increaseValue("true", "false");
                        }
                        else if (!originalClassList.contains(Class) && !predClassList.contains(Class)) {
                            //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                            cMatrix.increaseValue("false", "false");
                        }
                    }
                }
            }
            //########Iterate through Test Docs : Classify##########
            
            //########Report Results##########
            double FSum = 0;
            for (String Class : allClassList.split(",")) {
                cMatrix = cMatrices.get(Class);
                //System.out.println("Class : " + Class + "\tF-Measure: " + cMatrix.getMacroFMeasure());
                FSum += cMatrix.getMacroFMeasure();
                //System.out.println("Precision: " + cMatrix.getAvgPrecision());
                //System.out.println("Recall: " + cMatrix.getAvgRecall());
                
            }
            System.out.println("Macro F-Measure : " + FSum/(double)allClassList.split(",").length);
            //########Report Results##########
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        NaiveBayesClassifyRCV1 cl = new NaiveBayesClassifyRCV1();
        //String trainIndex = "/Users/sounakbanerjee/Desktop/Temp/index";
        String trainIndex = "/home/sounak/work/expesriment Byproducts/index/RCV1";
        //String testData = "/Volumes/Files/Current/Drive/Work/Experiment/Reuters21578-Apte-top10/training";
        String testData = "/home/sounak/work/Datasets/RCVsubsetTest";
        cl.performClassification(trainIndex, testData);
    }
}
