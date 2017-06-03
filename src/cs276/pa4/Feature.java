package cs276.pa4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs276.pa4.Scorers.AScorer;
import weka.core.Attribute;

public class Feature {

	public static boolean isSublinearScaling = true;
	private Parser parser = new Parser();
	double smoothingBodyLength = 800;

	Map<String,Double> idfs;

	// If you would like to use additional features, you can declare necessary variables here
	/*
	 * @TODO: Your code here
	 */

	public Feature(Map<String,Double> idfs){
		this.idfs = idfs;
	}

	public double[] extractFeatureVector(Document d, Query q){

		/* Compute doc_vec and query_vec */
		Map<String,Map<String, Double>> tfs = Util.getDocTermFreqs(d,q);  
		Map<String,Double> queryVector = getQueryVec(q);

		// normalize term-frequency
		this.normalizeTFs(tfs, d, q);

		/* [url, title, body, header, anchor] */
		double[] result = new double[5];
		for (int i = 0; i < result.length; i++) { result[i] = 0.0; }
		for (String queryWord : q.queryWords){
			double queryScore = queryVector.get(queryWord);
			result[0]  += tfs.get("url").get(queryWord) * queryScore;
			result[1]  += tfs.get("title").get(queryWord) * queryScore;
			result[2]  += tfs.get("body").get(queryWord) * queryScore;
			result[3]  += tfs.get("header").get(queryWord) * queryScore;
			result[4]  += tfs.get("anchor").get(queryWord) * queryScore;
		}

		return result;
	}

	/* Generate query vector */
	public Map<String,Double> getQueryVec(Query q) {
		/* Count word frequency within the query, in most cases should be 1 */

		Map<String, Double> tfVector = new HashMap<String, Double>();
		String[] wordInQuery = q.query.toLowerCase().split(" ");
		for (String word : wordInQuery){
			if (tfVector.containsKey(word))
				tfVector.put(word, tfVector.get(word) + 1);
			else
				tfVector.put(word, 1.0);
		}

		/* Sublinear Scaling */
		if(isSublinearScaling){
			for (String word : tfVector.keySet()) {
				tfVector.put(word, 1 + Math.log(tfVector.get(word)));
			}
		} 
		/* Compute idf vector */
		Map<String,Double> idfVector = new HashMap<String,Double>();

		for (String queryWord : q.queryWords) {
			if (this.idfs.containsKey(queryWord))
				idfVector.put(queryWord, this.idfs.get(queryWord));
			else {
				idfVector.put(queryWord, Math.log(98998.0)); /* Laplace smoothing */
			}

		}

		/* Do dot-product */
		Map<String, Double> queryVector = new HashMap<String, Double>();
		for (String word : q.queryWords) {
			queryVector.put(word, tfVector.get(word) * idfVector.get(word));
		}

		return queryVector;
	}

	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q)
	{
		double normalizationFactor = (double)(d.body_length) + (double)(smoothingBodyLength);

		for (String queryWord : q.queryWords)
			for (String tfType : tfs.keySet())
				tfs.get(tfType).put(queryWord, tfs.get(tfType).get(queryWord)/normalizationFactor);
	}

	public double[] extractMoreFeatures(Document d, Query q, List<Attribute> attributes, Map<Query,Map<String, Document>> dataMap, AScorer bm25Scorer, AScorer smallestWindowScorer) {

		Map<String, Double> featureValues = featureValues(d,q,dataMap, bm25Scorer, smallestWindowScorer);
		double[] basic = extractFeatureVector(d, q);
		double[] more = new double[attributes.size()];
		for (int i = 0; i < attributes.size(); i++) {
			more[i] = featureValues.get(attributes.get(i).name());
		}
		
		double[] result = new double[basic.length + more.length];
		Util.copyNElems(basic, result, 0, basic.length);
		Util.copyNElems(more, result, basic.length-1, more.length);
		return result;

	}
	
	private boolean isPdf(Document document) {
		return document.url.endsWith(".pdf");
	}

	private boolean isHtml(Document document) {
		return document.url.endsWith(".html");
	}
	
	private double titleLength(Document document) {
		return document.title.split(" ").length;
	}
	
	private double percentBodyTerms(Document d, Query q) {
		Set<String> queryWords = new HashSet<String>(q.queryWords());
		double totalWords = queryWords.size();
		if (d.body_hits != null) {
			queryWords.removeAll(d.body_hits.keySet());
		}
		
		return 1.0 - (queryWords.size() / totalWords);

	}
	
	private double percentTitleTerms(Document d, Query q) {
		Set<String> queryWords = new HashSet<String>(q.queryWords());
		double totalWords = queryWords.size();
		if (d.title != null) {
			Set<String> titleWords = new HashSet<String>(Arrays.asList(d.title.split(" ")));
			queryWords.removeAll(titleWords);
		}
		
		return 1.0 - (queryWords.size() / totalWords);
		
	}
	
	private double percentUrlTerms(Document d, Query q) {
		Set<String> queryWords = new HashSet<String>(q.queryWords());
		double totalWords = queryWords.size();
		if (d.url != null) {
			Set<String> urlTerms = new HashSet<String>(Arrays.asList(d.url.split("[^A-Za-z0-9\\s]")));
			queryWords.removeAll(urlTerms);
		}
		
		return 1.0 - (queryWords.size() / totalWords);
		
	}
	
	private double compound(Document d, Query q) {
		return percentUrlTerms(d, q) + percentTitleTerms(d, q) + percentBodyTerms(d, q);
	}
	
	private Map<String, Double> featureValues(Document d, Query q, Map<Query,Map<String, Document>> dataMap, AScorer bm25Scorer, AScorer smallestWindowScorer) {
		Map<String, Double> featureValues = new HashMap<String,Double>();
		featureValues.put(("bm25"), bm25Scorer.getSimScore(d, q));
		featureValues.put(("smallestwindow"), smallestWindowScorer.getSimScore(d, q));
		featureValues.put(("pagerank"), (double)d.page_rank);
		featureValues.put(("isPdf"), isPdf(d) ? 1.0 : 0);
		featureValues.put(("bodylength"), (double)d.body_length);
		featureValues.put(("titlelength"), titleLength(d));
		featureValues.put("percentUrlTerms", percentUrlTerms(d, q));
		featureValues.put("percentTitleTerms", percentTitleTerms(d, q));
		featureValues.put("percentBodyTerms", percentBodyTerms(d, q));
		featureValues.put("isHtml", isHtml(d) ? 1.0 : 0);
		featureValues.put("compound", compound(d, q));
		return featureValues;
	}
		

}
