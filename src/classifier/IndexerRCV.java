/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classifier;

//Read XML Files
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jdom2.Attribute;
//import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
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
public class IndexerRCV {

    //Read Reuters files and return the document data
    Document readRCV(String path) {
        Document luceneDoc = new Document();
        try {
            File inputFile = new File(path);
            SAXBuilder saxBuilder = new SAXBuilder();
            org.jdom2.Document document = saxBuilder.build(inputFile);
            //System.out.println("Root element :" + document.getRootElement().getName());

            Element rootElement = document.getRootElement();
            Attribute IDattribute = rootElement.getAttribute("itemid");

            //System.out.println("DocID : " + IDattribute.getValue() );
            String docID = IDattribute.getValue();
            luceneDoc.add(new StringField("DocID", docID, Field.Store.YES));
            //System.out.println("Title : " + rootElement.getChild("title").getText());
            String title = rootElement.getChild("title").getText();
            luceneDoc.add(new StringField("Title", title, Field.Store.YES));
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

            //Extract codes form the Document
            List<Element> MetaList = rootElement.getChild("metadata").getChildren();
            for (int i = 0; i < MetaList.size(); i++) {
                Element Meta = MetaList.get(i);
                if ("codes".equals(Meta.getName())) {
                    Attribute MetaAttribute = Meta.getAttribute("class");
                    String allCodes = "";
                    if (MetaAttribute.getValue().contains("topics")) {
                        List<Element> CodeList = Meta.getChildren();
                        for (int j = 0; j < CodeList.size(); j++) {
                            Element Code = CodeList.get(j);
                            Attribute codeAttribute = Code.getAttribute("code");
                            allCodes = allCodes + codeAttribute.getValue();
                            if (j != CodeList.size() - 1) {
                                allCodes = allCodes + ",";
                            }
                        }
                        //System.out.println(": " + allCodes);
                        luceneDoc.add(new StringField("Topics", allCodes, Field.Store.YES));
                    } else if (MetaAttribute.getValue().contains("countries")) {
                        List<Element> CodeList = Meta.getChildren();
                        for (int j = 0; j < CodeList.size(); j++) {
                            Element Code = CodeList.get(j);
                            Attribute codeAttribute = Code.getAttribute("code");
                            allCodes = allCodes + codeAttribute.getValue();
                            if (j != CodeList.size() - 1) {
                                allCodes = allCodes + ",";
                            }
                        }
                        //System.out.println(": " + allCodes);
                        luceneDoc.add(new StringField("Countries", allCodes, Field.Store.YES));
                    } else if (MetaAttribute.getValue().contains("industries")) {
                        List<Element> CodeList = Meta.getChildren();
                        for (int j = 0; j < CodeList.size(); j++) {
                            Element Code = CodeList.get(j);
                            Attribute codeAttribute = Code.getAttribute("code");
                            allCodes = allCodes + codeAttribute.getValue();
                            if (j != CodeList.size() - 1) {
                                allCodes = allCodes + ",";
                            }
                        }
                        //System.out.println(": " + allCodes);
                        luceneDoc.add(new StringField("Industries", allCodes, Field.Store.YES));
                    }
                }
            }
        } catch (JDOMException | IOException e) {
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
            String corpusFolder = "/Users/sounakbanerjee/Desktop/Temp/untitled folder";
            File folder = new File(corpusFolder);
            File[] listOfFiles = folder.listFiles();
            for (File file : listOfFiles) {
                try {
                    if (!file.getAbsolutePath().contains(".xml")) {
                        System.out.println("Unknown file: " + file.getAbsolutePath());
                        continue;
                    }
                    Document luceneDoc = readRCV(file.getAbsolutePath());
                    writer.addDocument(luceneDoc);
                } catch (MalformedInputException me) {
                    System.out.println("Error reading file: " + file.getAbsolutePath() + " :: Improper Encoding. ");
                    //continue;
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
        IndexerRCV ind = new IndexerRCV();
        ind.CreateIndex();
    }
}
