/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package CopulaResources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.file.Path;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;


import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;



/**
 *
 * @author sounakbanerjee
 */


public class TermCooccurence implements Serializable {
    
    
    HashMap<String, HashMap<TermPair, Integer>> CooccurenceList;
    
    public TermCooccurence() {
        this.CooccurenceList = new HashMap();
    }
    
    public TermCooccurence(Path saveDirectory) throws IOException, ClassNotFoundException {
        
        File dir = saveDirectory.toFile();
        File file = new File(dir, "CoOcc");
        if (!file.exists()) {
            throw new IOException("No Co-Occurence List file found. Filename should be \"CoOcc\"");
        }
        try (FileInputStream fileIn = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fileIn);) {
            this.CooccurenceList = (HashMap) in.readObject();
        }
    }
    
    public void increaseCount(TermPair tp, String tpClass) {
        if (tpClass == null || tpClass.equals("")) {
            tpClass = "no_class";
        }
        HashMap<TermPair, Integer> classFreqList = this.CooccurenceList.get(tpClass);
        if (classFreqList == null) 
            classFreqList = new HashMap();
        this.CooccurenceList.put(tpClass, classFreqList);
        Integer freq = classFreqList.get(tp);
        if (freq!=null)
            freq += 1;
        else
            freq = 1;
        classFreqList.put(tp, freq);
    }
    
    public void increaseCountbyn(TermPair tp, String tpClass, int n) {
        if (tpClass == null || tpClass.equals("")) {
            tpClass = "no_class";
        }
        HashMap<TermPair, Integer> classFreqList = this.CooccurenceList.get(tpClass);
        if (classFreqList == null) 
            classFreqList = new HashMap();
        this.CooccurenceList.put(tpClass, classFreqList);
        Integer freq = classFreqList.get(tp);
        if (freq!=null)
            freq += n;
        else
            freq = n;
        classFreqList.put(tp, freq);
    }
    
    
    public Integer getClassUniquePairCount (String tpClass) {
        if (tpClass == null || tpClass.equals("")) {
            tpClass = "no_class";
        }
        HashMap<TermPair, Integer> classFreqList = this.CooccurenceList.get(tpClass);
        if (classFreqList == null) 
            return null;
        return classFreqList.size();
    }
    
    
    public Integer getClassFreqSum (String tpClass) {
        Integer sum = 0;
        if (tpClass == null || tpClass.equals("")) 
            tpClass = "no_class";
        
        HashMap<TermPair, Integer> classFreqList = this.CooccurenceList.get(tpClass);
        if (classFreqList == null) 
            return null;
        
        for (Integer value : classFreqList.values()) 
            sum += value;
        
        return sum;
    }
    
    
    public Integer getCount(TermPair tp, String tpClass) {
        if (tpClass == null || tpClass.equals("")) 
            tpClass = "no_class";
        
        HashMap<TermPair, Integer> classFreqList = this.CooccurenceList.get(tpClass);
        if (classFreqList == null) 
            return null;
        
        return classFreqList.get(tp);
    }
    
    
    public String[] getClassList() {
        Set<String> keys = this.CooccurenceList.keySet();
        String[] classList = keys.toArray(new String[keys.size()]);
        return classList;
    }
    
    
    public static TermCooccurence generateCooccurencebyClass (IndexReader indexReader, String classFieldName, String textFieldName, Analyzer analyzer, int minFreq, int maxFreq) throws IOException {
        System.out.println(":::Generating Term-Pair List:::");
        TermCooccurence CooccurList = new TermCooccurence();
        Terms classes = MultiFields.getTerms(indexReader, classFieldName);
        if (classes != null) {
            TermsEnum classesEnum = classes.iterator();
            BytesRef nextClass;
            while ((nextClass = classesEnum.next()) != null) {
                if (nextClass.length > 0) {
                    Term term = new Term(classFieldName, nextClass);
                    String tpClass = nextClass.utf8ToString();
                    BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
                    booleanQuery.add(new BooleanClause(new TermQuery(term), BooleanClause.Occur.MUST));
                    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
                    TopDocs topDocs;
                    topDocs = indexSearcher.search(booleanQuery.build(), indexReader.numDocs());
                    
                    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                        IndexableField[] storableFields = indexSearcher.doc(scoreDoc.doc).getFields(textFieldName);
                        for (IndexableField singleStorableField : storableFields) {
                            if (singleStorableField != null) {
                                BytesRef text = new BytesRef(singleStorableField.stringValue());
                                generateCooccurences(text.utf8ToString(), analyzer, CooccurList, tpClass);
                            }
                        }
                    }
                }
            }
        }
        System.out.println(":::Generation Complete:::");
        return CooccurList;
    }
    
    
    public static void generateCooccurencebyClass (IndexReader indexReader, String classFieldName, String textFieldName, Analyzer analyzer, int minFreq, int maxFreq, Path saveDir) throws IOException {
        System.out.println(":::Generating Term-Pair List:::");
        TermCooccurence CooccurList = new TermCooccurence();
        Terms classes = MultiFields.getTerms(indexReader, classFieldName);
        if (classes != null) {
            TermsEnum classesEnum = classes.iterator();
            BytesRef nextClass;
            while ((nextClass = classesEnum.next()) != null) {
                if (nextClass.length > 0) {
                    Term term = new Term(classFieldName, nextClass);
                    String tpClass = nextClass.utf8ToString();
                    BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
                    booleanQuery.add(new BooleanClause(new TermQuery(term), BooleanClause.Occur.MUST));
                    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
                    TopDocs topDocs;
                    topDocs = indexSearcher.search(booleanQuery.build(), indexReader.numDocs());
                    
                    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                        IndexableField[] storableFields = indexSearcher.doc(scoreDoc.doc).getFields(textFieldName);
                        for (IndexableField singleStorableField : storableFields) {
                            if (singleStorableField != null) {
                                BytesRef text = new BytesRef(singleStorableField.stringValue());
                                generateCooccurences(text.utf8ToString(), analyzer, CooccurList, tpClass);
                            }
                        }
                    }
                }
            }
        }
        CooccurList.trim(minFreq, maxFreq);
        CooccurList.savetoFile(saveDir);
        System.out.println(":::Generation Complete:::");
    }
    
    
    private static void generateCooccurences(String text, Analyzer analyzer, TermCooccurence cooccurList, String tpClass) throws IOException {
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.US);
        sentences.setText(text);
        int start = sentences.first();
        for (int end = sentences.next(); end != BreakIterator.DONE; start = end, end = sentences.next()) {
            String sentence = text.substring(start,end);
            List words = tokenizeString(analyzer, sentence);
            for (int i = 0 ; i < words.size() ; i++) {
                for (int j = i+1 ; j < words.size() ; j++) {
                    TermPair tp = new TermPair(words.get(i).toString(), words.get(j).toString());
                    cooccurList.increaseCount(tp, tpClass);
                }
            }
        }
    }
    
    /**
     * Generate all sentence level Cooccurence of all terms in a piece of text along with their frequencies.
     * 
     * @param text The text to be processed
     * @param analyzer A Lucene analyzer object used to process the text.
     * @param tpClass An option, if the user would like to assign a class to the document, if not leave it "".
     * @return A TermCooccurence variable that contains list of all cooccurences
     * @throws java.io.IOException
     */
    public static HashMap<TermPair, Integer> generateCooccurences (String text, Analyzer analyzer) throws IOException {
        HashMap<TermPair, Integer> cooccurList = new HashMap();
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.US);
        sentences.setText(text);
        int start = sentences.first();
        for (int end = sentences.next(); end != BreakIterator.DONE; start = end, end = sentences.next()) {
            String sentence = text.substring(start,end);
            List words = tokenizeString(analyzer, sentence);
            for (int i = 0 ; i < words.size() ; i++) {
                for (int j = i+1 ; j < words.size() ; j++) {
                    TermPair tp = new TermPair(words.get(i).toString(), words.get(j).toString());
                    Integer freq = cooccurList.get(tp);
                    if (freq!=null)
                        freq += 1;
                    else
                        freq = 1;
                    cooccurList.put(tp, freq);;
                }
            }
        }
        return cooccurList;
    }
    
    
    private static List tokenizeString(Analyzer analyzer, String str) {
        List result = new ArrayList<>();
        try {
            TokenStream stream = analyzer.tokenStream(null, new StringReader(str));
            stream.reset();
            while (stream.incrementToken())
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
    
    
    public void trim (int minFreq, int maxFreq)  {
        for (String tpClass : this.CooccurenceList.keySet()) {
            HashMap<TermPair, Integer> classCooccurenceList = this.CooccurenceList.get(tpClass);
            Iterator<HashMap.Entry<TermPair, Integer>> tpIterator = classCooccurenceList.entrySet().iterator();
            while (tpIterator.hasNext()) {
                HashMap.Entry<TermPair, Integer> tp = tpIterator.next();
                if (tp.getValue() < minFreq || tp.getValue() > maxFreq)
                    tpIterator.remove();
            }
        }
    }
    
    
    public void savetoFile(Path saveDirectory) throws IOException{
        
        File dir = saveDirectory.toFile();
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, "CoOcc");
        if (file.exists()) {
            System.out.println("A Co-Occurence List already exists : attempting to overwrite.");
            Boolean del = file.delete();
            if (!del)
                throw new IOException("Cannot delete file, check permission!");
        }
        file.createNewFile();
        try (FileOutputStream fileOut = new FileOutputStream(file);  //"/tmp/employee.ser"
                ObjectOutputStream out = new ObjectOutputStream(fileOut);) {
            
            out.writeObject(this.CooccurenceList);
        }
        
    }
    
    
}
