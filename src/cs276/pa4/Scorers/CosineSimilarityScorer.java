package cs276.pa4.Scorers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cs276.pa4.Document;
import cs276.pa4.Query;

/**
 * Skeleton code for the implementation of a 
 * Cosine Similarity Scorer in Task 1.
 */
public class CosineSimilarityScorer extends AScorer {

	/*
	 * TODO: You will want to tune the values for
	 * the weights for each field.
	 */
	double urlweight = .1;
	double titleweight  = .1;
	double bodyweight = 0.02;
	double headerweight = .1;
	double anchorweight = .1;
	double smoothingBodyLength = 8000.0;

	/**
	 * Construct a Cosine Similarity Scorer.
	 * @param idfs the map of idf values
	 */
	double maxIdfVal = 1;
	public CosineSimilarityScorer(Map<String,Double> idfs) {
		super(idfs);
		this.maxIdfVal = idfs.values().stream().max((d1, d2) -> d1.compareTo(d2)).get();
	}

	/**
	 * Get the net score for a query and a document.
	 * @param tfs the term frequencies
	 * @param q the Query
	 * @param tfQuery the term frequencies for the query
	 * @param d the Document
	 * @return the net score
	 */
	public double getNetScore(Map<String, Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery, Document d) {
		tfQuery = idfWeightTfQuery(tfQuery);
		List<String> queryTermStructure = q.queryWords();

		List<Double> queryVector = new ArrayList<Double>();
		List<Double> documentVector = new ArrayList<Double>();

		for (String queryWord : queryTermStructure) {
			Double documentWordScore = 0.0;
			documentWordScore += tfs.get(URL).get(queryWord) * urlweight;
			documentWordScore += tfs.get(TITLE).get(queryWord) * titleweight;
			documentWordScore += tfs.get(BODY).get(queryWord) * bodyweight;
			documentWordScore += tfs.get(HEADER).get(queryWord) * headerweight;
			documentWordScore += tfs.get(ANCHOR).get(queryWord) * anchorweight;
			documentVector.add(documentWordScore);

			queryVector.add(tfQuery.get(queryWord));
		}
		return dotProduct(queryVector, documentVector);

		/*
		 * TODO : Your code here
		 * See Equation 2 in the handout regarding the net score
		 * between a query vector and the term score vectors
		 * for a document.
		 */
		//    return score;
	}
	
	private Map<String, Double> idfWeightTfQuery(Map<String,Double> tfQuery) {
		for (String queryWord : tfQuery.keySet()) {
			Double idfWeight = idfs.get(queryWord);
			idfWeight = idfWeight == null ? maxIdfVal: idfWeight;
			Double tfIdf = idfWeight * tfQuery.get(queryWord); // weight * count of term occurences in query
			tfQuery.put(queryWord, tfIdf);
		}
		return tfQuery;
	}

	private Double dotProduct(List<Double> v1, List<Double> v2) {
		double score = 0.0;
		if (v1.size() != v2.size()) System.out.println("Error in vector size!!");
		for (int i = 0; i < v1.size(); i++) {
			score += v1.get(i) * v2.get(i);
		}
		return score;
	}


	/**
	 * Normalize the term frequencies. 
	 * @param tfs the term frequencies
	 * @param d the Document
	 * @param q the Query
	 */
	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q) {
		/*
		 * TODO : Your code here
		 * Note that we should give uniform normalization to all 
		 * )fields as discussed in the assignment handout.
		 */
		// Note that we both sublinearly scale it AND normalize it here
		for (Map<String,Double> fieldTfs : tfs.values()) {

			for (String term : fieldTfs.keySet()) {
				Double count = fieldTfs.get(term);
				if (count != 0) {
					// sublinearly scale here
					count = 1 + Math.log(count);

					// normalize by bodycount here
					count = count / (d.body_length + smoothingBodyLength);
				}
				fieldTfs.put(term, count);
			}
		}
	}

	/**
	 * Write the tuned parameters of cosineSimilarity to file.
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
					"urlweight", "titleweight", "bodyweight", "headerweight", 
					"anchorweight", "smoothingBodyLength"
			};
			double[] values = {
					this.urlweight, this.titleweight, this.bodyweight, 
					this.headerweight, this.anchorweight, this.smoothingBodyLength
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
	/** Get the similarity score between a document and a query.
	 * @param d the Document
	 * @param q the Query
	 * @return the similarity score.
	 */
	public double getSimScore(Document d, Query q) {
		//    System.out.println("d: " + d.url + "(q: " + q + "), title: " + d.title);
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
		this.normalizeTFs(tfs, d, q);
		Map<String,Double> tfQuery = getQueryFreqs(q);

		// Write out tuned cosineSimilarity parameters
		// This is only used for grading purposes.
		// You should NOT modify the writeParaValues method.
		writeParaValues("cosinePara.txt");
		return getNetScore(tfs,q,tfQuery,d);
	}
}

