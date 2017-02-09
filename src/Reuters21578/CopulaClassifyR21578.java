package Reuters21578;


//For Classification
import CommonResources.ConfusionMatrix;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import java.util.Map;
import java.util.HashMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
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



public class CopulaClassifyR21578 {

    ClassificationResult<BytesRef> classifyDoc(KNearestNeighborDocumentClassifier knn, String path) {

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

            res = knn.assignClass(luceneDoc);

        } catch (IOException e) {
            e.printStackTrace();
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
            Map<String, Analyzer> field2analyzer = new HashMap<>();
            field2analyzer.put("Text", new org.apache.lucene.analysis.standard.StandardAnalyzer(RanksNL.stopWords));
            //field2analyzer.put("Text", new org.apache.lucene.analysis.en.EnglishAnalyzer());
            
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
            KNearestNeighborDocumentClassifier knn = new KNearestNeighborDocumentClassifier(atomicReader,
                    BM25, null, 10, 0, 0, "Topics", field2analyzer, "Text");
            //#######Read Index and Train#########
            
            
            /*
            //###########Get Class List of Training Docs##########
            Terms classes = MultiFields.getTerms(reader, "Topics");
            TermsEnum classesEnum = classes.iterator();
            BytesRef next;
            System.out.println("Classes : ");
            while ((next = classesEnum.next()) != null) {
                if (next.length > 0) {
                    String term = next.utf8ToString();
                    System.out.println(term + ",");
                }
            }
            //###########Get Class List of Training Docs##########
            */
            
            ConfusionMatrix cMatrix = new ConfusionMatrix();
            
            //########Iterate through Test Docs : Classify & Report##########
            //testData = "/Volumes/Files/Current/Drive/Work/Experiment/Reuters21578-Apte-top10/training";
            testData = "/home/sounak/work/Datasets/Reuters21578-Apte-top10/test";
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
                    ClassificationResult<BytesRef> res = classifyDoc(knn, file.getAbsolutePath());
                    BytesRef resClass = res.getAssignedClass();
                    String originalClass = classFolder.getName();
                    String predClass = resClass.utf8ToString();
                    //System.out.println("Predicted Class : " + predClass + "\tOriginal Class : " + originalClass);
                    //cMatrix.increment(originalClass, predClass);
                    cMatrix.increaseValue(originalClass, predClass);
                }
            }
            System.out.println("F-Measure: " + cMatrix.getMacroFMeasure());
            System.out.println("Precision: " + cMatrix.getAvgPrecision());
            System.out.println("Recall: " + cMatrix.getAvgRecall());
            
            //########Iterate through Test Docs : Classify & Report##########

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CopulaClassifyR21578 cl = new CopulaClassifyR21578();
        //cl.performClassification("/Users/sounakbanerjee/Desktop/Temp/index", "");
        cl.performClassification("/home/sounak/work/Datasets/index/reuters21578", "");
    }
}
