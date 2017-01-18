/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package reference_git;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.MalformedInputException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author sounak
 */
public class indexComparison {
    public static void main(String[] args) {
        HashMap<String, String> analyzers = new HashMap<>();
        analyzers.put("org.apache.lucene.analysis.core.KeywordAnalyzer", "keywordIndex" );
        analyzers.put("org.apache.lucene.analysis.core.SimpleAnalyzer", "simpleIndex" );
        analyzers.put("org.apache.lucene.analysis.core.StopAnalyzer", "stopIndex" );
        analyzers.put("org.apache.lucene.analysis.standard.StandardAnalyzer", "standardIndex" );
        for (String analyzer : analyzers.keySet()) {
            try {
                //Create a new lucene index;
                String indexDirectoryName = analyzers.get(analyzer);
                File inputDirectory = new File(indexDirectoryName);
                if (!inputDirectory.exists()) {
                    inputDirectory.mkdir();
                }
                String corpusFolder = "/home/shanmukh/apps/corpus/";
                Directory dir = FSDirectory.open(Paths.get(indexDirectoryName));

                IndexWriterConfig iwc = new IndexWriterConfig((Analyzer) Class.forName(analyzer).newInstance());
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                IndexWriter writer = new IndexWriter(dir, iwc);
                //Read XML Files
                File folder = new File(corpusFolder);
                File[] listOfFiles = folder.listFiles();
                for (int j = 0; j < listOfFiles.length; j++) {
                    File f = listOfFiles[j];
                    try {

                        if (f.getAbsolutePath().contains(".trectext") == false) {
                            System.out.println("is trectext file");
                            continue;
                        }
                        org.jdom2.Document xmlDoc = ParseInputFile(f);
                        //A signle xml document will contain multiple documents
                        //Read each single document and parse the contents to build the index/
                        org.jdom2.Element rootDocs = xmlDoc.getRootElement();
                        for (Element doc : rootDocs.getChildren("DOC")) {
                            //Get the attributes for each element and create an index
                            Document luceneDoc = new Document();
                            String docNo = GetChildNodeContent(doc, "DOCNO");
                            String head = GetChildNodeContent(doc, "HEAD");

                            String byline = GetChildNodeContent(doc, "BYLINE");


                            String dateline = GetChildNodeContent(doc, "DATELINE");
                            String text = GetChildNodeContent(doc, "TEXT");


                            luceneDoc.add(new StringField("DOCNO", docNo, Field.Store.YES));
                            luceneDoc.add(new StringField("HEAD", head, Field.Store.YES));
                            luceneDoc.add(new StringField("BYLINE", byline, Field.Store.YES));
                            luceneDoc.add(new StringField("DATELINE", dateline, Field.Store.YES));
                            luceneDoc.add(new TextField("TEXT", text, Field.Store.YES));


                            writer.addDocument(luceneDoc);


                        }

                    } catch (MalformedInputException me) {
                        System.out.println("Unable to read file " + f.getAbsolutePath() + ". Improper Encoding. ");
                        continue;
                    }
                }
                writer.close();
                //Print Index Summaries.
                String outputFilename = "log_" + analyzer + ".txt";

                GetSummary(indexDirectoryName, outputFilename);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
    public static void GetSummary(String indexPath, String outputFileName) throws Exception{
        PrintWriter pw = new PrintWriter(outputFileName);
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get((indexPath))));
        //Print the total number of documents in the corpus
        pw.println("Total number of documents in the corpus: " + reader.maxDoc());
        pw.println("Number of documents containing the term \"new\" for field \"TEXT\": "+reader.docFreq(new Term("TEXT", "new")));

        //Print the total number of occurrences of the term "new" across all documents for <field>TEXT</field>.
        pw.println("Number of occurrences of \"new\" in the field\"TEXT\": "+reader.totalTermFreq(new Term("TEXT","new")));

        Terms vocabulary = MultiFields.getTerms(reader, "TEXT");

        //Print the size of the vocabulary for <field>TEXT</field>, applicable when the index has only one segment.
        pw.println("Size of the vocabulary for this field:  " + vocabulary.size());
        //Print the total number of documents that have at least one term for <field>TEXT</field>
       pw.println("Number of documents that have at least one term for  this field: " + vocabulary.getDocCount());


        //Print the total number of tokens for <field>TEXT</field>
        pw.println("Number of tokens for this field " + vocabulary.getSumTotalTermFreq());

        //Print the total number of postings for <field>TEXT</field>
       pw.println("Number of postings for this field: "+vocabulary.getSumDocFreq());

        //Print the vocabulary for <field>TEXT</field>
        TermsEnum iterator = vocabulary.iterator();
        BytesRef byteRef = null;
        pw.close();
        reader.close();
    }
    public static String GetChildNodeContent ( Element e, String tag){
        //Read each doc xml element and get it's child contents ;
        String returnString = "";
        for ( Element child : e.getChildren(tag)){
            returnString = returnString  +  " " + child.getTextNormalize();
        }
        return returnString;
    }
    public static org.jdom2.Document ParseInputFile(File f) throws Exception {
        System.out.println("Parsing File : " + f.getAbsolutePath());
        StringBuilder sb = new StringBuilder();
        sb.append("<docs>");
        List<String> fileLines = new ArrayList<String>();
        String input = FileUtils.readFileToString(f);
        String pattern = "&(?!amp;)";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(input);
        input = input.replaceAll(pattern, "&amp;");
        sb.append(input);
        sb.append("</docs>");
        String xml = sb.toString();
        SAXBuilder xb = new SAXBuilder();
        org.jdom2.Document doc;
        doc = xb.build(new StringReader(xml));
        return doc;

    }
}