/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classifier;

//Read XML Files
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
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
import java.nio.charset.MalformedInputException;

//For Dumping
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author sounak
 */
public class IndexerR21578 {

    //Read Reuters files and return the document data
    Document readR21578(String path, String docClass) {
        Document luceneDoc = new Document();
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
                temp.append(line.trim() + " ");
            }
            //System.out.println("Text : \n");
            String text = temp.toString();
            luceneDoc.add(new TextField("Text", text, Field.Store.YES));

            luceneDoc.add(new StringField("Topics", docClass, Field.Store.YES));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return luceneDoc;
    }

    void CreateIndex() {
        try {
            //Create a new lucene index;
            String indexDirectoryName = "/Users/sounakbanerjee/Desktop/Temp/index";
            File indexDirectory = new File(indexDirectoryName);
            if (!indexDirectory.exists()) {
                indexDirectory.mkdir();
            }
            Directory dir = FSDirectory.open(Paths.get(indexDirectoryName));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(dir, iwc);
            //Read XML Files
            String corpusFolder = "/Volumes/Files/Current/Drive/Work/Experiment/Reuters21578-Apte-top10/training";
            File trainingFolder = new File(corpusFolder);
            File[] listOfFolders = trainingFolder.listFiles();
            for (File folder : listOfFolders) {
                File classFolder = new File(folder.getAbsolutePath());
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
    
/*
    void DumpIndex(String indexPath) throws Exception {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get((indexPath))));
        //Print the total number of documents in the corpus
        System.out.println("Total number of documents in the corpus: " + reader.maxDoc());

        System.out.println("Number of documents containing the term \"new\" for field \"TEXT\": " + reader.docFreq(new Term("TEXT", "new")));

        //Print the total number of occurrences of the term "new" across all documents for <field>TEXT</field>.
        System.out.println("Number of occurrences of \"new\" in the field\"TEXT\": " + reader.totalTermFreq(new Term("TEXT", "new")));

        Terms vocabulary = MultiFields.getTerms(reader, "TEXT");

        //Print the size of the vocabulary for <field>TEXT</field>, applicable when the index has only one segment.
        System.out.println("Size of the vocabulary for this field:  " + vocabulary.size());
        //Print the total number of documents that have at least one term for <field>TEXT</field>
        System.out.println("Number of documents that have at least one term for  this field: " + vocabulary.getDocCount());

        //Print the total number of tokens for <field>TEXT</field>
        System.out.println("Number of tokens for this field " + vocabulary.getSumTotalTermFreq());

        //Print the total number of postings for <field>TEXT</field>
        System.out.println("Number of postings for this field: " + vocabulary.getSumDocFreq());

        //Print the vocabulary for <field>TEXT</field>
        TermsEnum iterator = vocabulary.iterator();
        BytesRef byteRef = null;
        System.out.println("\n*******Vocabulary-End**********");
        reader.close();
    }
*/
    
    public static void main(String[] args) {
        IndexerR21578 ind = new IndexerR21578();
        ind.CreateIndex();
    }
}
