/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classifier;

import java.io.File;
import java.nio.charset.MalformedInputException;
import java.nio.file.Paths;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author sounak
 */
public class Indexer {
    public static void main(String[] args){
        try{
            //Create a new lucene index;
            String indexDirectoryName = "indexDir";
            File inputDirectory = new File(indexDirectoryName);
            if ( !inputDirectory.exists()){
                inputDirectory.mkdir();
            }
            String corpusFolder= "/home/shanmukh/apps/corpus/";
            Directory dir = FSDirectory.open(Paths.get(indexDirectoryName));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(dir, iwc);
            //Read XML Files
            File folder = new File(corpusFolder);
            File[] listOfFiles = folder.listFiles();
            for ( int j =0 ;  j < listOfFiles.length ; j++) {
                File f = listOfFiles[j];
                try {

                    if (f.getAbsolutePath().contains(".trectext") == false) {
                        System.out.println("trectext file");
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
                        luceneDoc.add(new TextField("TEXT", text ,Field.Store.YES));


                        writer.addDocument(luceneDoc);
                    }

                }
                catch(MalformedInputException me){
                    System.out.println("Unable to read file " + f.getAbsolutePath() + ". Improper Encoding. ");
                    continue;
                }
            }
            writer.close();
            //Print Index Summaries.
            //GetSummary(indexDirectoryName);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
