package cs276.pa4.Scorers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cs276.pa4.Document;
import cs276.pa4.Query;

/**
 * An abstract class for a scorer. 
 * Needs to be extended by each specific implementation of scorers.
 */
public abstract class AScorer {

	// Map: term -> idf
	Map<String,Double> idfs; 

	public static String URL = "url";
	public static String TITLE = "title";
	public static String BODY = "body";
	public static String HEADER = "header";
	public static String ANCHOR = "anchor";

	// Various types of term frequencies that you will need
	String[] TFTYPES = {URL,TITLE,BODY,HEADER,ANCHOR};

	/**
	 * Construct an abstract scorer with a map of idfs.
	 * @param idfs the map of idf scores
	 */
	public AScorer(Map<String,Double> idfs) {
		this.idfs = idfs;
	}

	/**
	 * You can implement your own function to whatever you want for debug string
	 * The following is just an example to include page information in the debug string
	 * The string will be forced to be 1-line and truncated to only include the first 200 characters
	 */
	public String getDebugStr(Document d, Query q)
	{
		return "Pagerank: " + Integer.toString(d.page_rank);
	}

	/**
	 * Score each document for each query.
	 * @param d the Document
	 * @param q the Query
	 */
	public abstract double getSimScore(Document d, Query q);

	/**
	 * Get frequencies for a query.
	 * @param q the query to compute frequencies for
	 */
	public Map<String,Double> getQueryFreqs(Query q) {

		// queryWord -> term frequency
		String[] queryArr = new String[q.queryWords().size()];
		queryArr = q.queryWords().toArray(queryArr);
		Map<String,Double> tfQuery = stringToCounts(queryArr);

		/*
		 * TODO : Your code here
		 * Compute the raw term (and/or sublinearly scaled) frequencies
		 * Additionally weight each of the terms using the idf value
		 * of the term in the query (we use the PA1 corpus to determine
		 * how many documents contain the query terms which is stored
		 * in this.idfs).
		 */
		return tfQuery;

	}


	/*
	 * TODO : Your code here
	 * Include any initialization and/or parsing methods
	 * that you may want to perform on the Document fields
	 * prior to accumulating counts.
	 * See the Document class in Document.java to see how
	 * the various fields are represented.
	 */


	/**
	 * Accumulate the various kinds of term frequencies 
	 * for the fields (url, title, body, header, and anchor).
	 * You can override this if you'd like, but it's likely 
	 * that your concrete classes will share this implementation.
	 * @param d the Document
	 * @param q the Query
	 * @throws UnsupportedEncodingException 
	 */
	public Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q) {

		// Map from tf type -> queryWord -> score
		Map<String,Map<String, Double>> tfs = new HashMap<String,Map<String, Double>>();

		/*
		 * TODO : Your code here
		 * Initialize any variables needed
		 */


		// Url first
		String decodedUrl = decodedUrl(d.url);
		String[] tokenizedUrl = decodedUrl.split("[^A-Za-z0-9\\s]");
		Map<String, Double> urlCounts = stringToCounts(tokenizedUrl);
		Map<String, Double> urlTermFreqs = tokenizedStrToQueryCounts(q, urlCounts);

		// Title next 
		String[] tokenizedTitle = d.title == null ? new String[]{} : d.title.split("\\s+");
		Map<String, Double> titleCounts = stringToCounts(tokenizedTitle);
		Map<String, Double> titleTermFreqs = tokenizedStrToQueryCounts(q, titleCounts);
		

		// Body hits
		Map<String, Double> bodyHitCounts = d.body_hits == null ? emptyMap :
			d.body_hits.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> (double) e.getValue().size()));
		Map<String, Double> bodyTermFreqs = tokenizedStrToQueryCounts(q, bodyHitCounts);
				

		// Header counts
		String headerConcat = d.headers != null ? String.join(" ", d.headers) : "";
		Map<String, Double> headerCounts = stringToCounts(headerConcat.split("\\s+"));
		Map<String, Double> headerTermFreqs = tokenizedStrToQueryCounts(q, headerCounts);

		// Lastly, anchor counts
		Map<String, Double> anchorTermFreqs = new HashMap<String,Double>();
		if (d.anchors != null) {
		for (String anchorText : d.anchors.keySet()) {
			String[] tokenizedAnchor = anchorText.split("\\s+");
			Set<String> anchorSet = new HashSet<String>(Arrays.asList(tokenizedAnchor));
			for (String queryWord : q.queryWords()) {
				if (anchorSet.contains(queryWord)) {
					Double currCount = anchorTermFreqs.get(queryWord);
					currCount = currCount == null ? 0 : currCount;
					anchorTermFreqs.put(queryWord, currCount + 1);
				}
			}
			
		}
		}
		anchorTermFreqs = tokenizedStrToQueryCounts(q, anchorTermFreqs);
		
		tfs.put(URL, urlTermFreqs);
		tfs.put(TITLE, titleTermFreqs);
		tfs.put(BODY, bodyTermFreqs);
		tfs.put(HEADER, headerTermFreqs);
		tfs.put(ANCHOR, anchorTermFreqs);
				
		return tfs;
	}
	
	public String decodedUrl(String url) {
		String decodedUrl = "";
		try {
			decodedUrl = URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return decodedUrl;
	}
	
	private static Map<String, Double> emptyMap = new HashMap<String,Double>();

	private Map<String, Double> tokenizedStrToQueryCounts(Query q, Map<String, Double> counts) {
		Map<String, Double> result = new HashMap<String, Double>();
		for (String queryWord : q.queryWords()) {
			Double count = counts.containsKey(queryWord) ? counts.get(queryWord) : 0.0;
			result.put(queryWord, count.doubleValue());
		}
		return result;
	}

	// Maps a term -> number of times it occurs in the url
	private Map<String, Double> stringToCounts(String[] tokenized) {
		Map<String, Double> result = new HashMap<String, Double>();
		for (String s : tokenized) {
			Double counts = result.get(s);
			if (counts != null) {
				result.put(s, counts + 1);
			} else {
				result.put(s,  1.0);
			}
		}
		return result;
	}

}

