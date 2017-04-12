package RCV1V2;

//Read XML Files
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.document.Document;

//For Indexing
import java.nio.file.Paths;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.FieldType;


/**
 *
 * @author sounakbanerjee
 */
public class IndexerRCV1V2 {
    
    
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
    
    
    void CreateIndex(String indexDirectoryName, String trainCorpus, String topics) {
        try {
            //Create a new lucene index;
            File indexDirectory = new File(indexDirectoryName);
            if (!indexDirectory.exists()) {
                indexDirectory.mkdir();
            }
            Directory dir = FSDirectory.open(Paths.get(indexDirectoryName));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(dir, iwc);
            
            //Read Files
            String[] textAll = Files.readAllLines(new File(trainCorpus).toPath()).toArray(new String[0]);
            HashMap<String, String> filesText = createTextMap(textAll);
            System.out.println("---Text Strings Generated---\nSize : " + filesText.size());
            String[] topicsAll = Files.readAllLines(new File(topics).toPath()).toArray(new String[0]);
            HashMap<String, List<String>> filesTopics = createTopicsMap(topicsAll);
            System.out.println("\n---Topic Lists Generated---\nSize : " + filesTopics.size());
            
            
            //For creating termvectors
            FieldType ft = new FieldType(TextField.TYPE_STORED);
            ft.setStoreTermVectors(true);
            ft.setStoreTermVectorOffsets(true);
            ft.setStoreTermVectorPositions(true);
            
            //Writing to Index
            System.out.println("\n---Writing To Index---");
            filesText.forEach((fileID, fileText) -> {
                try {
                    Document luceneDoc = new Document();
                    luceneDoc.add(new StringField("DocID", fileID, Field.Store.YES));
                    luceneDoc.add(new Field("Text", fileText, ft));
                    
                    //Write once for each topic
                    List<String> fileTopics = filesTopics.get(fileID);
                    if(fileTopics==null || fileTopics.isEmpty()) {
                        System.out.println("No topics on file: " + fileID);
                        return;
                    }
                    //System.out.print("\n" + fileID + " : " + "Topic List : " + fileTopics.size() + " : ");
                    for (String topic : fileTopics) {
                        Document temp = luceneDoc;
                        temp.add(new StringField("Topics", topic, Field.Store.YES));
                        writer.addDocument(temp);
                        //System.out.print(topic + ", ");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(IndexerRCV1V2.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            
            writer.close();
            System.out.println("\n---Index Created---");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main(String[] args) {
        IndexerRCV1V2 ind = new IndexerRCV1V2();
        
        //MacOS
        String indexDirectoryName = "/Users/sounakbanerjee/Desktop/Temp/index/RCV1V2";
        String trainCorpus = "/Volumes/Files/Work/Research/Information Retrieval/1) Data/Reuters/RCV/RCV/RCV1-V2/Raw Data/TrainingData/lyrl2004_tokens_train.dat";
        String topics = "/Volumes/Files/Work/Research/Information Retrieval/1) Data/Reuters/RCV/RCV/RCV1-V2/Category-File maps/rcv1-v2.topics.qrels";
        
        //Linux
        //String indexDirectoryName = "/home/sounak/work/expesriment Byproducts/index/RCV1";
        //String corpusFolder = "/home/sounak/work/Datasets/RCVsubset";
        
        ind.CreateIndex(indexDirectoryName, trainCorpus, topics);
    }
}