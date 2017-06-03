package cs276.pa4.Scorers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cs276.pa4.Document;
import cs276.pa4.Pair;
import cs276.pa4.Query;

/**
 * Skeleton code for the implementation of a BM25 Scorer in Task 2.
 */
public class BM25Scorer extends AScorer {

	/*
	 *  TODO: You will want to tune these values
	 */
	double urlweight = 1.25;
	double titleweight  = 1;
	double bodyweight = 0.025;
	double headerweight = 0.2;
	double anchorweight = 1.25;

	
	Map<String,Double> bm25FieldWeightMap;

	// BM25-specific weights
	
	double burl = 1.5;
	double btitle = 1.1;
	double bheader = 0.75;
	double bbody = 0.75;
	double banchor = 0.25;
	
	Map<String, Double> bm25BWeightMap; 
		
	double k1 = 1.0;
	double pageRankLambda = 1.5;
	double pageRankLambdaPrime = 1;
	double pageRankLambda2Prime = .8;
	
	int numDocs = 1;
	// query -> url -> document
	Map<Query,Map<String, Document>> queryDict; 

	// BM25 data structures--feel free to modify these
	// Document -> field -> length
	Map<Document,Map<String,Double>> lengths;  

	// field name -> average length
	Map<String,Double> avgLengths;    

	// Document -> pagerank score
	Map<Document,Double> pagerankScores; 
	
	// Pair(term, Document) -> weight (w[d,t])
	Map<Pair<String, Document>, Double> termDocWeightMap;
	
	double maxIdfVal = 1.0;

	/**
	 * Construct a BM25Scorer.
	 * @param idfs the map of idf scores
	 * @param queryDict a map of query to url to document
	 */
	public BM25Scorer(Map<String,Double> idfs, Map<Query,Map<String, Document>> queryDict) {
		super(idfs);
		this.queryDict = queryDict;
		this.initializeWeightMaps();
		this.calcAverageLengths();
		this.maxIdfVal = idfs.values().stream().max((d1, d2) -> d1.compareTo(d2)).get();
	}
	
	public void initializeWeightMaps() {
		termDocWeightMap = new HashMap<Pair<String, Document>, Double>();
		
		bm25BWeightMap = new HashMap<String,Double>();
		bm25BWeightMap.put(URL, burl);
		bm25BWeightMap.put(TITLE, btitle);
		bm25BWeightMap.put(HEADER, bheader);
		bm25BWeightMap.put(BODY, bbody);
		bm25BWeightMap.put(ANCHOR, banchor);
		
		bm25FieldWeightMap = new HashMap<String,Double>();
		bm25FieldWeightMap.put(URL, urlweight);
		bm25FieldWeightMap.put(TITLE, titleweight);
		bm25FieldWeightMap.put(HEADER, headerweight);
		bm25FieldWeightMap.put(BODY, bodyweight);
		bm25FieldWeightMap.put(ANCHOR, anchorweight);
	}

	/**
	 * Set up average lengths for BM25, also handling PageRank.
	 */
	public void calcAverageLengths() {
		lengths = new HashMap<Document,Map<String,Double>>();
		avgLengths = new HashMap<String,Double>();
		pagerankScores = new HashMap<Document,Double>();

		/*
		 * TODO : Your code here
		 * Initialize any data structures needed, perform
		 * any preprocessing you would like to do on the fields,
		 * handle pagerank, accumulate lengths of fields in documents.        
		 */

		


		/*
		 * TODO : Your code here
		 * Normalize lengths to get average lengths for
		 * each field (body, url, title, header, anchor)
		 */

		// initialize counter of lengths here, sum the lenghts
		// per field, then divide after the loop over all the docs
		for (String type : this.TFTYPES) {
			avgLengths.put(type, 0.0);
		}

		int docCounter = 0;
		double totalPageRank = 0;
		for (Query query : queryDict.keySet()) {
			for (String url : queryDict.get(query).keySet()) {
				Document doc = queryDict.get(query).get(url);

//				if (lengths.containsKey(doc)) continue;
				// maps field -> length of it (for this doc)
				Map<String, Double> fieldLengths = new HashMap<String,Double>();

				String decodedUrl = this.decodedUrl(doc.url);
				double urlLength = decodedUrl.split("[^A-Za-z0-9\\s]").length;
				double titleLength = doc.title != null ? doc.title.split("\\s+").length : 0;
				double headerLength = doc.headers != null ? numTokensInList(doc.headers) : 0;
				double bodyLength = doc.body_length;
				double anchorLength = doc.anchors != null ? numTokensInList(doc.anchors.keySet()) : 0;

				// Put the lengths for just the doc here
				fieldLengths.put(URL, urlLength);
				fieldLengths.put(TITLE, titleLength);
				fieldLengths.put(HEADER, headerLength);
				fieldLengths.put(BODY, bodyLength);
				fieldLengths.put(ANCHOR, anchorLength);
				lengths.put(doc, fieldLengths);
				totalPageRank += doc.page_rank;
				docCounter++;

				// Add the lengths in the global averager

				for (String type : this.TFTYPES) {
					double updatedTotal = avgLengths.get(type) + fieldLengths.get(type);
					avgLengths.put(type, updatedTotal);
				}
			}
		}
		for (String type : this.TFTYPES) {
			double fieldTotal = avgLengths.get(type);
			avgLengths.put(type, fieldTotal / docCounter);
//			avgLengths.put(type, fieldTotal / lengths.size());
		}
		numDocs = docCounter;
	}

	private Double numTokensInList(Collection<String> fieldList) {
		double length = 0;
		for (String fieldString : fieldList) {
			length += fieldString.split("\\s+").length;
		}
		return length;
	}

	/**
	 * Get the net score. 
	 * @param tfs the term frequencies
	 * @param q the Query 
	 * @param tfQuery
	 * @param d the Document
	 * @return the net score
	 */
	public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d) {
		double score = 0.0;

		/*
		 * TODO : Your code here
		 * Use equation 5 in the writeup to compute the overall score
		 * of a document d for a query q.
		 */
		
//		double nonTextualScore = pageRankLambda * Math.log10(pageRankLambdaPrime + d.page_rank);
//		double nonTextualScore = pageRankLambda * (1 / (pageRankLambdaPrime + Math.exp(-1 * d.page_rank * pageRankLambda2Prime)));
		double nonTextualScore = 0;
		
		for (String queryTerm : q.queryWords()) {
			double termWeight = termDocWeightMap.get(new Pair<String,Document>(queryTerm, d));

			Double idfWeight = idfs.get(queryTerm);
			if (idfWeight == null) {
//				idfWeight = maxIdfVal;
				idfWeight = Math.log(numDocs+1);
			}

			score += (termWeight * idfWeight) / (k1 + termWeight);
		}
		score += nonTextualScore;

		return score;
	}

	/**
	 * Do BM25 Normalization.
	 * @param tfs the term frequencies
	 * @param d the Document
	 * @param q the Query
	 */
	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q) {
		/*
		 * TODO : Your code here
		 * Use equation 3 in the writeup to normalize the raw term frequencies
		 * in fields in document d.
		 */


		for (String queryTerm : q.queryWords()) {
			double termDocWeight = 0;
			Pair<String, Document> queryDocPair = new Pair<String,Document>(queryTerm, d);

			for (String field : this.TFTYPES) {
				Double lenDocField = lengths.get(d).get(field);
				Double avgLenField = avgLengths.get(field);
				Double B = bm25BWeightMap.get(field);

				double rawFreq = tfs.get(field).get(queryTerm);
				
				double ftf = avgLenField == 0 || avgLenField == null ? 0 : 
					rawFreq / (1 + B * ((lenDocField / avgLenField) - 1));
				termDocWeight += bm25FieldWeightMap.get(field) * ftf;
			}
			
			termDocWeightMap.put(queryDocPair, termDocWeight);	
		}
	

	}

	/**
	 * Write the tuned parameters of BM25 to file.
	 * Only used for grading purpose, you should NOT modify this method.
	 * @param filePath the output file path.
	 */
	private void writeParaValues(String filePath) {
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			String[] names = {
					"urlweight", "titleweight", "bodyweight", 
					"headerweight", "anchorweight", "burl", "btitle", 
					"bheader", "bbody", "banchor", "k1", "pageRankLambda", "pageRankLambdaPrime"
			};
			double[] values = {
					this.urlweight, this.titleweight, this.bodyweight, 
					this.headerweight, this.anchorweight, this.burl, this.btitle, 
					this.bheader, this.bbody, this.banchor, this.k1, this.pageRankLambda, 
					this.pageRankLambdaPrime
			};
			BufferedWriter bw = new BufferedWriter(fw);
			for (int idx = 0; idx < names.length; ++ idx) {
				bw.write(names[idx] + " " + values[idx]);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	/**
	 * Get the similarity score.
	 * @param d the Document
	 * @param q the Query
	 * @return the similarity score
	 */
	public double getSimScore(Document d, Query q) {
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
		this.normalizeTFs(tfs, d, q);
		Map<String,Double> tfQuery = getQueryFreqs(q);

		// Write out the tuned BM25 parameters
		// This is only used for grading purposes.
		// You should NOT modify the writeParaValues method.
		writeParaValues("bm25Para.txt");
		return getNetScore(tfs,q,tfQuery,d);
	}

}

