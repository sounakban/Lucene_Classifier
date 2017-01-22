/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classifier;

/**
 *
 * @author sounakbanerjee
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.similarities.BM25Similarity;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.classification.document.KNearestNeighborDocumentClassifier;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.util.BytesRef;
//import org.apache.lucene.analysis.en.PorterStemFilter;

public class ClassifierR21578 {

    public void Classify(String indexLoc) {

        try {
            StandardAnalyzer analyzer1 = new StandardAnalyzer();
            FSDirectory index = FSDirectory.open(Paths.get(indexLoc));
            IndexReader reader = DirectoryReader.open(index);

            //Segmented reader
            List<LeafReaderContext> leaves = reader.leaves();
            BM25Similarity BM25 = new BM25Similarity();
            Map<String, Analyzer> field2analyzer = new HashMap<>();
            field2analyzer.put("Text", new org.apache.lucene.analysis.standard.StandardAnalyzer());
            /*For Multiple Leaves (Segment in Index)
            System.out.println("Num of leaves: " + leaves.size());
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
            
            
            //###Read current Doc###
            String path = "/Volumes/Files/Current/Drive/Work/Experiment/Reuters21578-Apte-top10/test/crude/0009605";
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
            //System.out.println("Text : \n");
            String text = temp.toString();
            luceneDoc.add(new TextField("Text", text, Field.Store.YES));

            //luceneDoc.add(new StringField("Topics", docClass, Field.Store.YES));
            //###Read current Doc###

            ClassificationResult<BytesRef> res = knn.assignClass(luceneDoc);
            BytesRef resstr = res.getAssignedClass();
            System.out.println("Assigned Class : " + resstr.utf8ToString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClassifierR21578 cl = new ClassifierR21578();
        cl.Classify("/Users/sounakbanerjee/Desktop/Temp/index");
    }
}
