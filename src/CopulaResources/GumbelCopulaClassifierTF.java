package CopulaResources;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;

import org.apache.lucene.classification.Classifier;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;


/**
 *
 * @author sounakbanerjee
 */




public class GumbelCopulaClassifierTF implements Classifier<BytesRef>{
    
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
    
    
    public GumbelCopulaClassifierTF(IndexReader indexReader, Analyzer analyzer, Query query, String classFieldName, TermCooccurence cooccurenceTrainData, String... textFieldNames) {
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
                    Double clVal = Math.log(getPrior(term, numDocsWithClass)) + getLogLikelihood(cooccuringTerms, term, numDocsWithClass);
                    //System.out.println("Class : " + next.utf8ToString() + "\tValue : " + clVal);
                    assignedClasses.add(new ClassificationResult<>(term.bytes(), -clVal));
                }
            }
        }
        // normalization; the values transforms to a 0-1 range
        return normClassificationResults(assignedClasses);
        //return assignedClasses;
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
    
    private double getLogLikelihood(HashMap<TermPair, Integer> cooccuringTerms, Term term, int docsWithClass) throws IOException {
        double result = 1;
        String currClass = term.text();
        HashMap<String, Double> wordFreq = new HashMap();
        HashMap<String, Double> wordWeight = new HashMap();
        HashMap<TermPair, Double> corCoeffList = new HashMap();
                
        // classVocabulary : calculate the total dictionary size (with freq) in documents of class c (+|V|)
        double classVocabulary = getIndexTermFreqForClass(term, wordFreq) + docsWithClass;
        //System.out.println("All classes : " + Arrays.toString(cooccurenceTrainData.getClassList()));
        //System.out.println("Current class : " + currClass + "\tFreq : " + cooccurenceTrainData.getClassFreqSum(currClass));
        double classtpVocabulary = (double) cooccurenceTrainData.getClassFreqSum(currClass).intValue();
        
        
        cooccuringTerms.forEach((tp, tpdocfreq) -> {
            Integer tpTrainFreq = cooccurenceTrainData.getCount(tp, currClass);
            String word1 = tp.getTerm1();
            String word2 = tp.getTerm2();
            Double word1Freq = wordFreq.get(word1);
            Double word2Freq = wordFreq.get(word2);
            if (word1Freq != null && word2Freq != null && tpTrainFreq != null) {
                
                // count the no of times the word appears in documents of class c (+1 for laplace smoothing)
                // P(w|c) = num/totFreq
                Double word1Weight = wordWeight.get(word1);
                if (word1Weight == null) {
                    //tf
                    Double word1Probability = (Double) (wordFreq.get(word1) + 1) / classVocabulary;
                    word1Weight = word1Probability;
                    wordWeight.put(word1, word1Weight);
                }
                Double word2Weight = wordWeight.get(word2);
                if (word2Weight == null) {
                    //tf
                    Double word2Probability = (Double) (wordFreq.get(word2) + 1) / classVocabulary;
                    word2Weight = word2Probability;
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
            
            double termPairCopulaValue = gumbelInversePhi(gumbelPhi(word1Weight, theta) + gumbelPhi(word2Weight, theta) , theta);
            termPairCopulaValueList.add(termPairCopulaValue);
        }
        
        for (double val : termPairCopulaValueList) {
            Double temp = Math.log(val);
            if (!temp.isNaN())
                result += temp;
        }
        
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
    
    private double getIndexTermFreqForClass(Term term, HashMap<String, Double> uniqueTerms) throws IOException {
        double totalSize = 0;
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        booleanQuery.add(new BooleanClause(new TermQuery(term), BooleanClause.Occur.MUST));
        TopDocs topDocs;
        topDocs = indexSearcher.search(booleanQuery.build(), indexReader.numDocs());
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            for (String textFieldName : textFieldNames) {
                IndexableField[] storableFields = indexSearcher.doc(scoreDoc.doc).getFields(textFieldName);
                for (IndexableField singleStorableField : storableFields) {
                    if (singleStorableField != null) {
                        BytesRef text = new BytesRef(singleStorableField.stringValue());
                        try (TokenStream stream = analyzer.tokenStream(null, new StringReader(text.utf8ToString()))){
                            stream.reset();
                            while (stream.incrementToken()){
                                String word = stream.getAttribute(CharTermAttribute.class).toString();
                                Double freq = uniqueTerms.get(word);
                                if (freq!=null)
                                    freq += 1;
                                else
                                    freq = 1d;
                                uniqueTerms.put(word, freq);
                                totalSize += 1;
                            }
                            stream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        //uniqueTerms.addAll(Arrays.asList(text.utf8ToString().replaceAll("[^a-zA-Z ]", "").toLowerCase().split(" ")));
                    }
                }
            }
        }
        return totalSize;
        //return (double)uniqueTerms.size() ; // number of unique terms in text fields of all docs of class c
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