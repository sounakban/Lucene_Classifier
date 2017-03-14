package CopulaResources;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;

import org.apache.lucene.classification.Classifier;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;


/**
 *
 * @author sounakbanerjee
 */




public class GumbelCopulaClassifierTFIDF implements Classifier<BytesRef>{
    
    /**
     * {@link org.apache.lucene.index.IndexReader} used to access the {@link org.apache.lucene.classification.Classifier}'s
     * index
     */
    protected final IndexReader indexReader;
    
    /**
     * names of the fields to be used as input text
     */
    protected final String[] textFieldNames;
    
    /**
     * name of the field to be used as a class / category output
     */
    protected final String classFieldName;
    
    /**
     * {@link org.apache.lucene.analysis.Analyzer} to be used for tokenizing unseen input text
     */
    protected final Analyzer analyzer;
    
    /**
     * {@link org.apache.lucene.search.IndexSearcher} to run searches on the index for retrieving frequencies
     */
    protected final IndexSearcher indexSearcher;
    
    /**
     * {@link org.apache.lucene.search.Query} used to eventually filter the document set to be used to classify
     */
    protected final Query query;
    
    
    protected final TermCooccurence cooccurenceTrainData;
    
    
    /**
     * Creates a new GumbelCopula classifier.
     *
     * @param indexReader     the reader on the index to be used for classification
     * @param analyzer       an {@link Analyzer} used to analyze unseen text
     * @param query          a {@link Query} to eventually filter the docs used for training the classifier, or {@code null}
     *                       if all the indexed docs should be used
     * @param classFieldName the name of the field used as the output for the classifier NOTE: must not be havely analyzed
     *                       as the returned class will be a token indexed for this field
     * @param cooccurenceTrainData a TermCooccurence object generated from the training index.
     * @param textFieldNames the name of the fields used as the inputs for the classifier, NO boosting supported per field
     */
    
    
    public GumbelCopulaClassifierTFIDF(IndexReader indexReader, Analyzer analyzer, Query query, String classFieldName, TermCooccurence cooccurenceTrainData, String... textFieldNames) {
        this.indexReader = indexReader;
        this.indexSearcher = new IndexSearcher(this.indexReader);
        this.textFieldNames = textFieldNames;
        this.classFieldName = classFieldName;
        this.analyzer = analyzer;
        this.query = query;
        this.cooccurenceTrainData = cooccurenceTrainData;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ClassificationResult<BytesRef> assignClass(String inputDocument) throws IOException {
        List<ClassificationResult<BytesRef>> assignedClasses = assignClassList(inputDocument);
        ClassificationResult<BytesRef> assignedClass = null;
        double maxscore = -Double.MAX_VALUE;
        for (ClassificationResult<BytesRef> c : assignedClasses) {
                //System.out.println(c.getAssignedClass().utf8ToString() + ":" + c.getScore());
            if (c.getScore() > maxscore) {
                assignedClass = c;
                maxscore = c.getScore();
            }
        }
        return assignedClass;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<ClassificationResult<BytesRef>> getClasses(String inputDocument) throws IOException {
        List<ClassificationResult<BytesRef>> assignedClasses = assignClassList(inputDocument);
        Collections.sort(assignedClasses);
        return assignedClasses;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<ClassificationResult<BytesRef>> getClasses(String inputDocument, int max) throws IOException {
        List<ClassificationResult<BytesRef>> assignedClasses = assignClassList(inputDocument);
        Collections.sort(assignedClasses);
        return assignedClasses.subList(0, max);
    }
    
    
    /**
     * Calculate probabilities for all classes for a given input text
     * @param inputDocument the input text as a {@code String}
     * @return a {@code List} of {@code ClassificationResult}, one for each existing class
     * @throws IOException if assigning probabilities fails
     */
    protected List<ClassificationResult<BytesRef>> assignClassList(String inputDocument) throws IOException {
        //System.out.println("GumbelCopulaClassifierTF.assignClassList");
        List<ClassificationResult<BytesRef>> assignedClasses = new ArrayList<>();
        Terms classes = MultiFields.getTerms(indexReader, classFieldName);
        if (classes != null) {
            TermsEnum classesEnum = classes.iterator();
            BytesRef next;
            HashMap<TermPair, Integer> cooccuringTerms = TermCooccurence.generateCooccurences(inputDocument, this.analyzer);
            int numDocsWithClass = indexReader.getDocCount(classFieldName);
            while ((next = classesEnum.next()) != null) {
                if (next.length > 0) {
                    Term term = new Term(this.classFieldName, next);
                    double prior = Math.log(getPrior(term, numDocsWithClass));
                    double logLikelihood = getLogLikelihood(cooccuringTerms, term);        
                    Double clVal = prior + logLikelihood;
                    //System.out.println("Class : " + next.utf8ToString() + "\tScore : " + clVal + "\tPrior : " + prior + "\tLikelihood : " + logLikelihood);
                    assignedClasses.add(new ClassificationResult<>(term.bytes(), -clVal));
                }
            }
        }
        
        //return assignedClasses;
        // normalization; the values transforms to a 0-1 range
        return normClassificationResults(assignedClasses);
    }
    
    
    /**
     * tokenize a <code>String</code> on this classifier's text fields and analyzer
     *
     * temporarily not used
     *
     * @param text the <code>String</code> representing an input text (to be classified)
     * @return a <code>String</code> array of the resulting tokens
     * @throws IOException if tokenization fails
     *
     * protected String[] tokenize(String text) throws IOException {
     * Collection<String> result = new LinkedList<>();
     * for (String textFieldName : textFieldNames) {
     * try (TokenStream tokenStream = analyzer.tokenStream(textFieldName, text)) {
     * CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
     * tokenStream.reset();
     * while (tokenStream.incrementToken()) {
     * result.add(charTermAttribute.toString());
     * }
     * tokenStream.end();
     * }
     * }
     * return result.toArray(new String[result.size()]);
     * }
     */
    
    
    private double getPrior(Term term, int docsWithClassSize) throws IOException {
        Double result = ((double) indexReader.docFreq(term)/docsWithClassSize);
        if (!result.isNaN())
            return result;
        else
            return 0d;
    }
    
    private double getLogLikelihood(HashMap<TermPair, Integer> cooccuringTerms, Term term) throws IOException {
        double result = 1;
        String currClass = term.text();
        HashMap<String, Double> wordFreq = new HashMap();
        HashMap<String, Double> worddocFreq = new HashMap();
        double[] passbyRef = {0};
        HashMap<String, Double> wordWeight = new HashMap();
        HashMap<TermPair, Double> corCoeffList = new HashMap();
                
        // classVocabulary : calculate the total dictionary size (with freq) in documents of class c (+|V|)
        double classVocabulary = getIndexTermFreqForClass(cooccuringTerms, term, wordFreq, worddocFreq, passbyRef);
        final double numofDocsinClass = passbyRef[0];
        double classtpVocabulary = (double) cooccurenceTrainData.getClassFreqSum(currClass);
        //System.out.println(classtpVocabulary);
        
        
        cooccuringTerms.forEach((tp, tpdocfreq) -> {
            Integer tpTrainFreq = cooccurenceTrainData.getCount(tp, currClass);
            String word1 = tp.getTerm1();
            String word2 = tp.getTerm2();
            Double word1Freq = wordFreq.get(word1);
            Double word2Freq = wordFreq.get(word2);
            //System.out.println(word1 + " : " + word1Freq + " ;;; " + word2 + " : " + word2Freq);
            if (word1Freq != null && word1Freq != 0 && word2Freq != null && word2Freq != 0 && tpTrainFreq != null && tpTrainFreq != 0) {
                
                // count the no of times the word appears in documents of class c (+1 for laplace smoothing)
                // P(w|c) = num/totFreq
                Double word1Weight = wordWeight.get(word1);
                if (word1Weight == null) {
                    //tf
                    //System.out.println(wordFreq.get(word1));
                    Double word1TF = (Double) (wordFreq.get(word1) + 1) / classVocabulary;
                    //idf
                    Double word1IDF = Math.log((1 + numofDocsinClass) / (1 + worddocFreq.get(word1)));
                    
                    word1Weight = word1TF * word1IDF;
                    //System.out.println(numofDocsinClass + " : " + worddocFreq.get(word1));
                    //System.out.println(word1TF + " : " + word1IDF);
                    
                    wordWeight.put(word1, word1Weight);
                }
                Double word2Weight = wordWeight.get(word2);
                if (word2Weight == null) {
                    //tf
                    //System.out.println(wordFreq.get(word2));
                    Double word2TF = (Double) (wordFreq.get(word2) + 1) / classVocabulary;
                    //idf
                    Double word2IDF = Math.log((1 + numofDocsinClass) / (1 + worddocFreq.get(word2)));
                    
                    word2Weight = word2TF * word2IDF;
                    
                    wordWeight.put(word2, word2Weight);
                }
                
                Double tpProbability = tpTrainFreq / classtpVocabulary;
                
                //Jaccard(t1,t2) = (t1 intersection t2) /(t1 union t2)
                double jaccardCoeff = tpTrainFreq / (word1Freq + word2Freq - tpTrainFreq);
                corCoeffList.put(tp, jaccardCoeff);
                
                //PMI(t1,t2) = (P(t1)*P(t2)) /P(t1 union t2)
                //double PMICoeff = tpProbability / (word1Weight + word2Weight - tpProbability);
                //corCoeffList.put(tp, PMICoeff);
            }
        });
        
        if (corCoeffList.size() > 0)
            normalizeCorelationCoefficients(corCoeffList);
        
        //Calculate actual Likelihood C(u1,u2,theta) = exp( (( log(u1))^theta + ( log(u2))^theta)^(1/theta) )
        ArrayList<Double> termPairCopulaValueList = new ArrayList();
        for (HashMap.Entry<TermPair, Double> entry : corCoeffList.entrySet()) {
            TermPair tp = entry.getKey();
            double theta = entry.getValue();
            String word1 = tp.getTerm1();
            Double word1Weight = wordWeight.get(word1);
            String word2 = tp.getTerm2();
            Double word2Weight = wordWeight.get(word2);
            
            //System.out.println(word1Weight + " : " + word2Weight + " : " + theta);
            double termPairCopulaValue = gumbelInversePhi(gumbelPhi(word1Weight, theta) + gumbelPhi(word2Weight, theta) , theta);
            termPairCopulaValueList.add(termPairCopulaValue);
        }
        
        for (double val : termPairCopulaValueList) {
            //System.out.println(val);
            Double temp = Math.log(val);
            if (!temp.isNaN())
                //System.out.println(temp);
                result += temp;
        }
        
        //System.out.println(result);
        return result;
    }
    
    private void normalizeCorelationCoefficients(HashMap<TermPair, Double> corCoeffList) {
        double meanCorCoeff = 0;
        
        for (HashMap.Entry<TermPair, Double> entry : corCoeffList.entrySet()) {
            meanCorCoeff += entry.getValue();
        }
        meanCorCoeff /= corCoeffList.size();
        
        for (HashMap.Entry<TermPair, Double> entry : corCoeffList.entrySet()) {
            double corCoeff = entry.getValue();
            if (corCoeff < meanCorCoeff)
                entry.setValue(1d);
            else
                entry.setValue(corCoeff / meanCorCoeff);
        }
    }
    
    private double gumbelPhi (double termWeight, double corCoeff) {
        return Math.pow(-Math.log(termWeight), corCoeff);
    }
    
    private double gumbelInversePhi (double termWeight, double corCoeff) {
        return Math.exp(-Math.pow(termWeight, 1/corCoeff));
    }
    
    
    
    /**
     * Returns the average number of unique terms times the number of docs belonging to the input class
     * @param term the term representing the class
     * @return the average number of unique terms
     * @throws IOException if a low level I/O problem happens
     */
    private double getIndexTermFreqForClass(HashMap<TermPair, Integer> cooccuringTerms, Term term, 
            HashMap<String, Double> uniqueTerms, HashMap<String, Double> worddocFreq, double[] numofDocsinClass) throws IOException {
        
        cooccuringTerms.forEach((tp, tpdocfreq) -> {
            String word1 = tp.getTerm1();
            String word2 = tp.getTerm2();
            uniqueTerms.put(word1, 0d);
            uniqueTerms.put(word2, 0d);
            worddocFreq.put(word1, 0d);
            worddocFreq.put(word2, 0d);
        });
        double totalSize = 0;
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        booleanQuery.add(new BooleanClause(new TermQuery(term), BooleanClause.Occur.MUST));
        TopDocs topDocs;
        topDocs = indexSearcher.search(booleanQuery.build(), indexReader.numDocs());
        numofDocsinClass[0] = topDocs.totalHits;
        
        
        //Term Frequency
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            for (String textFieldName : textFieldNames) {
                Terms termVector;
                if ((termVector = indexReader.getTermVector(scoreDoc.doc, textFieldName)) == null) {
                    System.out.println("Skipped [in GumbelCopulaClassifierTFIDF]: " + scoreDoc.doc);
                    System.out.println("Make sure you indexed term vectors for text fields.");
                    continue;
                }
                    
                TermsEnum itr = termVector.iterator();
                BytesRef word = null;
                PostingsEnum postings = null;
                while((word = itr.next()) != null){
                    try {
                        //System.out.println("Word : " + word.utf8ToString());
                        String termText = word.utf8ToString();
                        if (uniqueTerms.containsKey(termText)) {
                            postings = itr.postings(postings, PostingsEnum.FREQS);
                            postings.nextDoc();
                            int currFreq = postings.freq();
                            //System.out.println("Word : " + termText);
                            //System.out.println("Freq : " + currFreq);
                            double existingFreq = uniqueTerms.get(termText);
                            uniqueTerms.put(termText, existingFreq + currFreq);
                            totalSize += currFreq;
                        }
                    } catch(Exception e){
                        System.out.println(e);
                    }
                }
            }
        }
        
        
        //Document Frequency
        for (String word : worddocFreq.keySet()) {
            BooleanQuery.Builder subQuery = new BooleanQuery.Builder();
            for (String textFieldName : textFieldNames) {
                subQuery.add(new BooleanClause(new TermQuery(new Term(textFieldName, word)), BooleanClause.Occur.MUST));
            }
            subQuery.add(new BooleanClause(booleanQuery.build(), BooleanClause.Occur.MUST));
            TopDocs wordDocs = indexSearcher.search(subQuery.build(), indexReader.numDocs());
            double docFreq = wordDocs.totalHits;
            worddocFreq.put(word, docFreq);
        }
        
        //System.out.println(totalSize);
        return totalSize;
    }
    
    
    
    /**
     * Normalize the classification results based on the max score available
     * @param assignedClasses the list of assigned classes
     * @return the normalized results
     */
    protected ArrayList<ClassificationResult<BytesRef>> normClassificationResults(List<ClassificationResult<BytesRef>> assignedClasses) {
        // normalization; the values transforms to a 0-1 range
        ArrayList<ClassificationResult<BytesRef>> returnList = new ArrayList<>();
        if (!assignedClasses.isEmpty()) {
            Collections.sort(assignedClasses);
            // this is a negative number closest to 0 = a
            double smax = assignedClasses.get(0).getScore();
            double smin = assignedClasses.get(assignedClasses.size()-1).getScore();
            double dinominator = smax - smin;
            //System.out.println(smax);
            
            for (ClassificationResult<BytesRef> cr : assignedClasses) {
                double numerator = cr.getScore() - smin;
                double score = numerator / dinominator;
                //System.out.print(scoreDiff + ",");
                //System.out.print(Math.exp(scoreDiff) + ";;");
                returnList.add(new ClassificationResult<>(cr.getAssignedClass(), score));
            }
            
            //System.out.println("\n--------------------------------");
        }
        return returnList;
    }
    
}