package Reuters21578;

//Read XML Files
import CommonResources.RanksNL;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import org.apache.lucene.document.Document;

//For Indexing
import java.nio.file.Paths;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author sounakbanerjee
 */
public class IndexerR21578 {
    
    //Read Reuters files and return the document data
    Document readR21578(String path, String docClass) {
        Document luceneDoc = new Document();
        
        //For creating termvectors
        FieldType ft = new FieldType(TextField.TYPE_STORED);
        ft.setStoreTermVectors(true);
        ft.setStoreTermVectorOffsets(true);
        ft.setStoreTermVectorPositions(true);
        
        try {
            File inputFile = new File(path);
            FileReader inputFileReader = new FileReader(inputFile);
            BufferedReader read = new BufferedReader(inputFileReader);
            
            //System.out.println("DocID : " + inputFile.getName() );
            String docID = inputFile.getName();
            luceneDoc.add(new StringField("DocID", docID, Field.Store.YES));
            
            String line = "";
            while (line.equals(""))    line = read.readLine();
            //System.out.println("Headline : "+ line.replaceAll("\\<.*?\\> ", "").trim());
            String headline = line.replaceAll("\\<.*?\\> ", "").trim();
            luceneDoc.add(new StringField("Headline", headline, Field.Store.YES));

            StringBuffer temp = new StringBuffer();
            while ((line = read.readLine()) != null) {
                temp.append(line.trim()).append(" ");
            }
            String text = temp.toString();
            //System.out.println("Text : \n");
            //luceneDoc.add(new TextField("Text", text, Field.Store.YES));      //Index without term-vectors
            luceneDoc.add(new Field("Text", text, ft));                         //Index with term-vectors

            luceneDoc.add(new StringField("Topics", docClass, Field.Store.YES));
            //System.out.println("Doc Class : " + docClass);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return luceneDoc;
    }

    void CreateIndex(String indexDirectoryName, String corpusFolder) {
        try {
            //Create a new lucene index;
            File indexDirectory = new File(indexDirectoryName);
            if (!indexDirectory.exists()) {
                indexDirectory.mkdir();
            }
            Directory dir = FSDirectory.open(Paths.get(indexDirectoryName));
            Analyzer analyzer = new StandardAnalyzer(RanksNL.stopWords);
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            
            //iwc.setCodec(new Lucene62Codec());
            IndexWriter writer = new IndexWriter(dir, iwc);
            
            //Read XML Files
            File trainingFolder = new File(corpusFolder);
            File[] listOfFolders = trainingFolder.listFiles();
            for (File classFolder : listOfFolders) {
                //File classFolder = new File(folder.getAbsolutePath());
                File[] listOfFiles = classFolder.listFiles();
                for (File file : listOfFiles) {
                    try {
                        if (file.getName().contains("\\.")) {
                            System.out.println("Unknown File: " + file.getAbsolutePath());
                            continue;
                        }
                        Document luceneDoc = readR21578(file.getAbsolutePath(), classFolder.getName());
                        
                        writer.addDocument(luceneDoc);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        IndexerR21578 ind = new IndexerR21578();
        
        //For Mac
        //String corpusFolder = "/Volumes/Files/Current/Drive/Work/Experiment/Reuters21578-Apte-top10/training";
        //String indexDirectoryName = "/Users/sounakbanerjee/Desktop/Temp/index";
        
        //For Linux
        String indexDirectoryName = "/home/sounak/work/expesriment Byproducts/index/reuters21578";
        String corpusFolder = "/home/sounak/work/Datasets/Reuters21578-Apte-top10/training";
        
        ind.CreateIndex(indexDirectoryName, corpusFolder);
    }
}
