package RCV1;

//Read XML Files
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jdom2.Attribute;
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
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableField;


/**
 *
 * @author sounakbanerjee
 */
public class IndexerRCV1 {

    //Read Reuters files and return the document data
    Document readRCV(String path, IndexWriter writer) {
        Document luceneDoc = new Document();
        
        //For creating termvectors
        FieldType ft = new FieldType(TextField.TYPE_STORED);
        ft.setStoreTermVectors(true);
        ft.setStoreTermVectorOffsets(true);
        ft.setStoreTermVectorPositions(true);
        
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
            //luceneDoc.add(new TextField("Text", text, Field.Store.YES));      //Index without term-vectors
            luceneDoc.add(new Field("Text", text, ft));                         //Index with term-vectors

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

    void CreateIndex(String indexDirectoryName, String corpusFolder) {
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
            
            //Read XML Files
            File folder = new File(corpusFolder);
            File[] listOfFolders = folder.listFiles();
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
                    try {
                        if (!(file.getName().contains(".xml") || file.getName().contains(".XML"))) {
                            System.out.println("Unknown file: " + file.getAbsolutePath());
                            continue;
                        }
                        Document luceneDoc = readRCV(file.getAbsolutePath(), writer);
                        IndexableField topics = luceneDoc.getField("Topics");
                        if(topics==null || topics.stringValue()==null || topics.stringValue().equals("")) {
                            System.out.println("No topics on file: " + file.getAbsolutePath());
                            continue;
                        }
                        luceneDoc.removeField("Topics");
                        for (String topic : topics.stringValue().split(",")) {
                            Document temp = luceneDoc;
                            temp.add(new StringField("Topics", topic, Field.Store.YES));
                            writer.addDocument(temp);
                        }
                    } catch (MalformedInputException me) {
                        System.out.println("Error reading file: " + file.getAbsolutePath() + " :: Improper Encoding. ");
                    }
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public static void main(String[] args) {
        IndexerRCV1 ind = new IndexerRCV1();
        
        //MacOS
        String indexDirectoryName = "/Users/sounakbanerjee/Desktop/Temp/index/RCV1";
        String corpusFolder = "/Volumes/Files/Work/Research/Information Retrieval/1) Data/Reuters/RCV1/Manual Subsets/RCV1 n-Docs/rcv1_topics100";
        
        //Linux
        //String indexDirectoryName = "/home/sounak/work/expesriment Byproducts/index/RCV1";
        //String corpusFolder = "/home/sounak/work/Datasets/RCVsubset";
        
        ind.CreateIndex(indexDirectoryName, corpusFolder);
    }
}