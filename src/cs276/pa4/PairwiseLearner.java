package cs276.pa4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;

/**
 * Implements Pairwise learner that can be used to train SVM
 *
 */
public class PairwiseLearner extends Learner {
	private LibSVM model;
	private int classFlipper = 1;
	public PairwiseLearner(boolean isLinearKernel){
		try{
			model = new LibSVM();
		} catch (Exception e){
			e.printStackTrace();
		}

		if(isLinearKernel){
			model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR, LibSVM.TAGS_KERNELTYPE));
		}
	}

	public PairwiseLearner(double C, double gamma, boolean isLinearKernel){
		try{
			model = new LibSVM();
		} catch (Exception e){
			e.printStackTrace();
		}

		model.setCost(C);
		model.setGamma(gamma); // only matter for RBF kernel
		if(isLinearKernel){
			model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR, LibSVM.TAGS_KERNELTYPE));
		}
	}

	@Override
	public Instances extractTrainFeatures(String train_data_file,
			String train_rel_file, Map<String, Double> idfs) {
		/*
		 * @TODO: Your code here:
		 * Get signal file 
		 * Construct output dataset of type Instances
		 * Add new attribute  to store relevance in the train dataset
		 * Populate data
		 */

		Instances instances = null;
		
		/* Build X and Y matrices */
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("url_w"));
		attributes.add(new Attribute("title_w"));
		attributes.add(new Attribute("body_w"));
		attributes.add(new Attribute("header_w"));
		attributes.add(new Attribute("anchor_w"));
//		attributes.add(new Attribute("classification"));

		ArrayList<String> classLabels = new ArrayList<String>();
	    classLabels.add("+1");
	    classLabels.add("-1");
	    Attribute relevance = new Attribute("relevance", classLabels);
	    attributes.add(relevance);

		instances = new Instances("train_dataset", attributes, 0);
		int numAttributes = instances.numAttributes();
		instances.setClassIndex(numAttributes-1);

		try {
			Quad<Instances, List<Pair<Query, Document>>, ArrayList<Attribute>, Map<Query, Map<Document, Integer>>> signalInfo = Util.loadSignalFile(train_data_file, train_rel_file, idfs, true);
			Instances queryDocVectors = signalInfo.getFirst();
			Map<Query, Map<Document, Integer>> indexMap = signalInfo.getFourth();
			
			Map<String, Map<String, Double>> relData = Util.loadRelData(train_rel_file);

			for (Query query : indexMap.keySet()) {

				// Get list of query -> List<Document, Relevance> (sorted by relevance score) from map
				List<Pair<String,Double>> urlRelevanceList = relData.get(query.query).entrySet().stream()
					.map(e -> new Pair<String,Double>(e.getKey(), e.getValue()))
					.collect(Collectors.toList());

				// Sort the docs by relevance rating
				urlRelevanceList.sort((pair1, pair2) -> pair1.getSecond() > pair2.getSecond() ? -1 : 1);
				
				// Iterate and create the pairwise docs
				for (int i = 0; i < urlRelevanceList.size(); i++) {
					Document higherRatedDoc = new Document(urlRelevanceList.get(i).getFirst());
					
					for (int j = i; j < urlRelevanceList.size(); j++) {
						// Same relevance means do not make pairwise ranking
						if (urlRelevanceList.get(i).getSecond() == urlRelevanceList.get(j).getSecond()) continue;
								
						// Just getting the feature vecs for the two instances here
						Document lowerRatedDoc = new Document(urlRelevanceList.get(j).getFirst());
						
						int higherFeatureIndex = indexMap.get(query).get(higherRatedDoc);
						int lowerFeatureIndex = indexMap.get(query).get(lowerRatedDoc);
						
						double[] higherRatedFeatureVec = queryDocVectors.instance(higherFeatureIndex).toDoubleArray();
						double[] lowerRatedFeatureVec = queryDocVectors.instance(lowerFeatureIndex).toDoubleArray();
						
						double[] differenceVector = new double[numAttributes];
						
						// Only go until the instance feature vector - 1 here because we don't need relevance score here
						for (int k = 0; k < higherRatedFeatureVec.length-1;k++) {
							double diff = classFlipper == 1 ? higherRatedFeatureVec[k] - lowerRatedFeatureVec[k] : lowerRatedFeatureVec[k] - higherRatedFeatureVec[k];
							differenceVector[k] = diff;
						}
						
						// Set class variable here
						String classification = classFlipper == 1 ? "+1" : "-1";
						differenceVector[numAttributes - 1] = instances.attribute(numAttributes-1).indexOfValue(classification);
//						differenceVector[numAttributes-1] = classFlipper;
						classFlipper *= -1;
						
						Instance newInstance = new DenseInstance(1.0, differenceVector);
						instances.add(newInstance);
					}
				}
			}
			return instances;
		} catch(IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	@Override
	public Classifier training(Instances dataset) {
		/*
		 * @TODO: Your code here
		 * Build classifer
		 */
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
		 * Use this to build the test features that will be used for testing
		 */
		TestFeatures testFeatures = new TestFeatures();
		try {
			Quad<Instances, List<Pair<Query, Document>>, ArrayList<Attribute>, Map<Query, Map<Document, Integer>>> testSignals = 
					Util.loadSignalFile(test_data_file, null, idfs, true);
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
		// For the docs returned for each query:
		// Put the docs in a list with their Instance
		// Then, lets sort the list with the comparator 
		Map<Query, List<Document>> result = new HashMap<Query, List<Document>>();
		int numAttributes = tf.features.numAttributes();
		for (Query query : tf.index_map.keySet()) {
			List<Pair<Document, Instance>> documentInstances = new ArrayList<Pair<Document,Instance>>();
			for (Document doc : tf.index_map.get(query).keySet()) {
				int index = tf.index_map.get(query).get(doc);
				Instance testInstance = tf.features.instance(index);
				documentInstances.add(new Pair<Document, Instance>(doc, testInstance));





			}
			documentInstances.sort((pair1, pair2) -> {
				try {
					Instance differenceVector = subtractFeatureVectors(pair1.getSecond(), pair2.getSecond(), numAttributes);
					differenceVector.setDataset(tf.features);
					double predIdx = model.classifyInstance(differenceVector);
					return predIdx == 0 ? 1 : -1;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return 0;
				}
			});
			
			List<Document> rankedDocs = documentInstances.stream().map((docInstancePair) -> docInstancePair.getFirst()).collect(Collectors.toList());
			result.put(query, rankedDocs);
		}
		return result;
	}
	private Instance subtractFeatureVectors(Instance vector1, Instance vector2, int numAttributes) {
		double[] differenceVector = new double[numAttributes];
		
		// Only go until the instance feature vector - 1 here because we don't need relevance score here
		for (int k = 0; k < numAttributes-1;k++) {
			differenceVector[k] = vector1.value(k) - vector2.value(k);
		}
		return new DenseInstance(1.0, differenceVector);
	}

}
