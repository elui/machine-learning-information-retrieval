package cs276.pa4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Implements point-wise learner that can be used to implement logistic regression
 *
 */
public class PointwiseLearner extends Learner {

	@Override
	public Instances extractTrainFeatures(String train_data_file,
			String train_rel_file, Map<String, Double> idfs) {

		/*
		 * @TODO: Below is a piece of sample code to show 
		 * you the basic approach to construct a Instances 
		 * object, replace with your implementation. 
		 */

		try {
			Quad<Instances, List<Pair<Query, Document>>, ArrayList<Attribute>, Map<Query, Map<Document, Integer>>> signalInfo = Util.loadSignalFile(train_data_file, train_rel_file, idfs, false, false);
			return signalInfo.getFirst();
		} catch(IOException ioe) {
			ioe.printStackTrace();
			return null;
		}


	}

	@Override
	public Classifier training(Instances dataset) {
		/*
		 * @TODO: Your code here
		 */

		LinearRegression model = new LinearRegression();
		try {
			model.buildClassifier(dataset);
			return model;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public TestFeatures extractTestFeatures(String test_data_file,
			Map<String, Double> idfs) {

		/*
		 * @TODO: Your code here
		 * Create a TestFeatures object
		 * Build attributes list, instantiate an Instances object with the attributes
		 * Add data and populate the TestFeatures with the dataset and features
		 */
		TestFeatures testFeatures = new TestFeatures();
		try {
			Quad<Instances, List<Pair<Query, Document>>, ArrayList<Attribute>, Map<Query, Map<Document, Integer>>> testSignals = 
					Util.loadSignalFile(test_data_file, null, idfs, false, false);
			testFeatures.features = testSignals.getFirst();
			testFeatures.index_map = testSignals.getFourth();
			return testFeatures;
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public Map<Query, List<Document>> testing(TestFeatures tf,
			Classifier model) {
		/*
		 * @TODO: Your code here
		 */
		
		Map<Query, List<Document>> result = new HashMap<Query, List<Document>>();
		for (Query query : tf.index_map.keySet()) {
			List<Pair<Document, Double>> documentScores = new ArrayList<Pair<Document,Double>>();
			for (Document doc : tf.index_map.get(query).keySet()) {
				int index = tf.index_map.get(query).get(doc);
				Instance testInstance = tf.features.instance(index);
				try {
					double predictedRelevance = model.classifyInstance(testInstance);
					documentScores.add(new Pair<Document, Double>(doc, predictedRelevance));
				} catch (Exception e) {
					
				}
			}
			documentScores.sort((pair1, pair2) -> pair1.getSecond() > pair2.getSecond() ? -1 : 1);
			List<Document> rankedDocs = documentScores.stream().map((docScorePair) -> docScorePair.getFirst()).collect(Collectors.toList());
			result.put(query, rankedDocs);
		}
		return result;
	}

}
