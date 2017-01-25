/**
 * Index RCV1 document collections
 */

package Others;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
//import static common.CommonVariables.FIELD_BOW;
//import static common.CommonVariables.FIELD_ID;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.document.FieldType;
import reference_dw.CommandLineIndexing;
/*import org.apache.lucene.document.TextField;*/


/*
#############For Stopword removal
public static String removeStopWords(String textFile) throws Exception {
    CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
    TokenStream tokenStream = new StandardTokenizer(Version.LUCENE_48, new StringReader(textFile.trim()));

    tokenStream = new StopFilter(Version.LUCENE_48, tokenStream, stopWords);
    StringBuilder sb = new StringBuilder();
    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();
    while (tokenStream.incrementToken()) {
        String term = charTermAttribute.toString();
        sb.append(term + " ");
    }
    return sb.toString();
}
*/






/**
 *
 * @author sounakbanerjee
 */
public class RCVDocIterator implements Iterator<Document> {

	protected BufferedReader rdr;
	protected boolean at_eof = false;
        Analyzer    analyzer;
        String      toStore;
        String      dumpPath;
        Properties  prop;

	public RCVDocIterator(File file) throws FileNotFoundException {
            rdr = new BufferedReader(new FileReader(file));
	}

	public RCVDocIterator(File file, CommandLineIndexing obj) throws FileNotFoundException, IOException {
            rdr = new BufferedReader(new FileReader(file));
            this.analyzer = obj.analyzer;
            this.prop = prop;
	}

	public RCVDocIterator(File file, Analyzer analyzer, Properties prop) throws FileNotFoundException, IOException {
            rdr = new BufferedReader(new FileReader(file));
            this.analyzer = analyzer;
            this.prop = prop;
	}

        public RCVDocIterator(File file, Analyzer analyzer, String toStore, String dumpPath) throws FileNotFoundException{
            rdr = new BufferedReader(new FileReader(file));
            this.analyzer = analyzer;
            this.toStore = toStore;
            this.dumpPath = dumpPath;
        }

        @Override
	public boolean hasNext() {
            return !at_eof;
	}

        
        /**
         * Removes the HTML tags from 'str' and returns the resultant string
         * @param str
         * @return 
         
        
        public String removeHTMLTags(String str) {
            String tagPatternStr = "<[^>\\n]*[>\\n]";
            Pattern tagPattern = Pattern.compile(tagPatternStr);

            Matcher m = tagPattern.matcher(str);
            return m.replaceAll(" ");
        }
        */


        /**
         * Returns the next document in the collection, setting FIELD_ID and FIELD_BOW.
         * @return 
         */
	@Override
	public Document next() {
            Document doc = new Document();
            StringBuffer sb = new StringBuffer();

            
        /*
            // +++ For replacing characters- ':','_'
            Map<String, String> replacements = new HashMap<String, String>() {{
                put(":", " ");
                put("_", " ");
            }};
            // create the pattern joining the keys with '|'
            String regExp = ":|_";
            Pattern p = Pattern.compile(regExp);
            // --- For replacing characters- ':','_'

        */
        
            try {
                String line;
                boolean in_doc = false;

                while (true) {
                    line = rdr.readLine();

                    if (line == null) {
                        at_eof = true;
                        break;
                    }
                    else
                        line = line.trim();
                    
                    // Remove Tag Lines
                    if (line.startsWith(ReutersTags.RCV_FILE_START))
                        continue;

                    // <DOC>
                    if (!in_doc) {
                        if (line.startsWith(ReutersTags.RCV_FILE_START))
                            in_doc = true;
                        else
                            continue;
                    }
                    if (line.contains(ReutersTags.RCV_FILE_END)) {
                        in_doc = false;
                        sb.append(line);
                        break;
                    }

                    // Id
                    if(line.startsWith(ReutersTags.RCV_INFO)) {
                        String str = line.split(" ")[1];
                        str = str.replace(ReutersTags.RCV_DOCID, "").replace("\"", "").trim();
                        doc.add(new StringField(ReutersFields.DOCID, str, Field.Store.YES));
                        continue;   // start reading the next line
                    }

                    // Title
                    if(line.startsWith(ReutersTags.RCV_TITLE_START)) {
                        String str = line.replace(ReutersTags.RCV_TITLE_START, "").replace(ReutersTags.RCV_TITLE_END, "").trim();
                        doc.add(new StringField(ReutersFields.TITLE, str, Field.Store.YES));
                        continue;   // start reading the next line
                    }

                    // Headline
                    if(line.startsWith(ReutersTags.RCV_HEADLINE_START)) {
                        String str = line.replace(ReutersTags.RCV_HEADLINE_START, "").replace(ReutersTags.RCV_HEADLINE_END, "").trim();
                        doc.add(new StringField(ReutersFields.HEADLINE, str, Field.Store.YES));
                        continue;   // start reading the next line
                    }

                    // Topic
                    if(line.startsWith(ReutersTags.RCV_CODES_TOPIC)) {
                        StringBuilder str = new StringBuilder("");
                        while (!line.contains(ReutersTags.RCV_CODES_END)) {
                            line = rdr.readLine();
                            if(line.startsWith(ReutersTags.RCV_CODES)) {
                                str.append(line.replace(ReutersTags.RCV_CODES, "").replace("\"", "").replace(">", "").trim());
                                str.append(",");
                            }
                        }
                        str.deleteCharAt(str.length()-1);         //Remove last comma
                        String save = str.toString();
                        doc.add(new StringField(ReutersFields.CODES_TOPIC, save, Field.Store.YES));
                        continue;   // start reading the next line
                    }

                    // Country
                    if(line.startsWith(ReutersTags.RCV_CODES_COUNTRY)) {
                        StringBuilder str = new StringBuilder("");
                        while (!line.contains(ReutersTags.RCV_CODES_END)) {
                            line = rdr.readLine();
                            if(line.startsWith(ReutersTags.RCV_CODES)) {
                                str.append(line.replace(ReutersTags.RCV_CODES, "").replace("\"", "").replace(">", "").trim());
                                str.append(",");
                            }
                        }
                        str.deleteCharAt(str.length()-1);         //Remove last comma
                        String save = str.toString();
                        doc.add(new StringField(ReutersFields.CODES_COUNTRY, save, Field.Store.YES));
                        continue;   // start reading the next line
                    }

                    // Industry
                    if(line.startsWith(ReutersTags.RCV_CODES_INDUSTRY)) {
                        StringBuilder str = new StringBuilder("");
                        while (!line.contains(ReutersTags.RCV_CODES_END)) {
                            line = rdr.readLine();
                            if(line.startsWith(ReutersTags.RCV_CODES)) {
                                str.append(line.replace(ReutersTags.RCV_CODES, "").replace("\"", "").replace(">", "").trim());
                                str.append(",");
                            }
                        }
                        str.deleteCharAt(str.length()-1);         //Remove last comma
                        String save = str.toString();
                        doc.add(new StringField(ReutersFields.CODES_INDUSTRY, save, Field.Store.YES));
                        continue;   // start reading the next line
                    }

                    sb.append(line);
                    sb.append(" ");
                }

                
                if (sb.length() > 0) {
                /*
                    //String txt = removeHTMLTags(sb.toString()); // remove all html-like tags (e.g. <xyz>)
                    //txt = removeURL(txt);

                    // +++ For replacing characters- ':','_'
                    StringBuffer temp = new StringBuffer();
                    Matcher m = p.matcher(txt);
                    while (m.find()) {
                        String value = replacements.get(m.group(0));
                        if(value != null)
                            m.appendReplacement(temp, value);
                    }
                    m.appendTail(temp);
                    txt = temp.toString();
                    // --- For replacing characters- ':','_'
                    */
                    String txt = sb.toString();
                    txt = txt.replace(ReutersTags.RCV_PARA_BEGIN, "");
                    txt = txt.replace(ReutersTags.RCV_PARA_END, "");
                    
                    StringBuffer tokenizedContentBuff = new StringBuffer();

                    TokenStream stream = analyzer.tokenStream(ReutersFields.BODY, 
                        new StringReader(txt));
                    CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
                    stream.reset();

                    while (stream.incrementToken()) {
                        String term = termAtt.toString();
                        tokenizedContentBuff.append(term).append(" ");
                    }

                    stream.end();
                    stream.close();

                    String toIndex = tokenizedContentBuff.toString();
                    //Lucene 4.2.0
                    //doc.add(new Field(FIELD_BOW, toIndex, Field.Store.valueOf(toStore), Field.Index.ANALYZED, Field.TermVector.YES));
                    FieldType fldTyp = new FieldType();
                    fldTyp.setStored(true);
                    fldTyp.setTokenized(true);
                    fldTyp.setStoreTermVectors(true);
                    doc.add(new Field(ReutersFields.BODY, toIndex, fldTyp));

                    if(null != dumpPath) {
                        FileWriter fw = new FileWriter(dumpPath, true);
                        BufferedWriter bw = new BufferedWriter(fw);
                        bw.write(toIndex+"\n");
                        bw.close();
                    }
                }
            } catch (IOException e) {
                doc = null;
            }
            return doc;
	}

        /**
         * This is only used for dumping the content of the files; NO INDEXING
         * @return 
         
	public Document dumpNextDoc() {
            Document doc = new Document();
            StringBuffer sb = new StringBuffer();

            /*
            // +++ For replacing characters- ':','_'
            Map<String, String> replacements = new HashMap<String, String>() {{
                put(":", " ");
                put("_", " ");
            }};
            // create the pattern joining the keys with '|'
            String regExp = ":|_";
            Pattern p = Pattern.compile(regExp);
            // --- For replacing characters- ':','_'
            

            try {
                String line;
                boolean in_doc = false;
                String doc_no = null;

                while (true) {
                    line = rdr.readLine();

                    if (line == null) {
                        at_eof = true;
                        break;
                    }
                    else
                        line = line.trim();

                    // <DOC>
                    if (!in_doc) {
                        if (line.startsWith("<DOC>"))
                            in_doc = true;
                        else
                            continue;
                    }
                    if (line.startsWith("</DOC>")) {
                        in_doc = false;
                        sb.append(line);
                        break;
                    }
                    // </DOC>

                    // <DOCNO>
                    if(line.startsWith("<DOCNO>")) {
                        doc_no = line;
                        while(!line.endsWith("</DOCNO>")) {
                            line = rdr.readLine().trim();
                            doc_no = doc_no + line;
                        }
                        doc_no = doc_no.replace("<DOCNO>", "").replace("</DOCNO>", "").trim();
                        doc.add(new StringField(FIELD_ID, doc_no, Field.Store.YES));
                        continue;   // start reading the next line
                    }
                    // </DOCNO>

                    sb.append(" ");
                    sb.append(line);
                }
                if (sb.length() > 0) {
                    String txt = removeHTMLTags(sb.toString()); // remove all html tags
                    txt = removeURL(txt);

                    // +++ For replacing characters- ':','_'
                    StringBuffer temp = new StringBuffer();
                    Matcher m = p.matcher(txt);
                    while (m.find()) {
                        String value = replacements.get(m.group(0));
                        if(value != null)
                            m.appendReplacement(temp, value);
                    }
                    m.appendTail(temp);
                    txt = temp.toString();
                    // --- For replacing characters- ':','_'

                    StringBuffer tokenizedContentBuff = new StringBuffer();

                    TokenStream stream = analyzer.tokenStream(ReutersFields.BODY, new StringReader(txt));
                    CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
                    stream.reset();

                    while (stream.incrementToken()) {
                        String term = termAtt.toString();
                        tokenizedContentBuff.append(term).append(" ");
                    }

                    stream.end();
                    stream.close();

                    FileWriter fileWritter = new FileWriter(prop.getProperty("dumpPath"), true);
                    BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                    bufferWritter.write(tokenizedContentBuff.toString()+"\n");
                    bufferWritter.close();
                }
            } catch (IOException e) {
                doc = null;
            }
            return doc;
	}
*/
        @Override
	public void remove() {
	}
}
