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
//import org.apache.lucene.analysis.en.PorterStemFilter;

public class ClassifierRCV {

    public void Classify(String indexLoc) {

        try {
            StandardAnalyzer analyzer1 = new StandardAnalyzer();
            FSDirectory index = FSDirectory.open(Paths.get(indexLoc));
            IndexReader reader = DirectoryReader.open(index);

            //Segmented reader
            List<LeafReaderContext> leaves = reader.leaves();
            BM25Similarity BM25 = new BM25Similarity();
            Map<String, Analyzer> field2analyzer = new HashMap<>();
            field2analyzer.put("text", new org.apache.lucene.analysis.standard.StandardAnalyzer());
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
            Document luceneDoc = new Document();
            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClassifierRCV cl = new ClassifierRCV();
        cl.Classify("/Users/sounakbanerjee/Desktop/Temp/index");
    }
}
