package ws.palladian.classification.nominal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;

/**
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class NominalClassifier implements Classifier<NominalClassifierModel> {

    @Override
    public NominalClassifierModel train(List<Instance> instances) {

        CountMatrix<String> cooccurrenceMatrix = CountMatrix.create();

        for (Instance instance : instances) {
            String className = instance.getTargetClass();
            List<NominalFeature> nominalFeatures = instance.getFeatureVector().getAll(NominalFeature.class);
            for (NominalFeature nominalFeature : nominalFeatures) {
                cooccurrenceMatrix.add(className, nominalFeature.getValue());
            }
        }

        return new NominalClassifierModel(cooccurrenceMatrix);
    }

    @Override
    public NominalClassifierModel train(Dataset dataset) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CategoryEntries classify(FeatureVector vector, NominalClassifierModel model) {

        CountMatrix<String> cooccurrenceMatrix = model.getCooccurrenceMatrix();

        // category-probability map, initialized with zeros
        Map<String, Double> scores = LazyMap.create(ConstantFactory.create(0.));

        // category names
        Set<String> categories = cooccurrenceMatrix.getKeysX();

        for (NominalFeature nominalFeature : vector.getAll(NominalFeature.class)) {

            for (String category : categories) {

                String featureValue = nominalFeature.getValue();
                int cooccurrences = cooccurrenceMatrix.getCount(category, featureValue);
                int rowSum = cooccurrenceMatrix.getRowSum(featureValue);

                double score = (double)cooccurrences / rowSum;
                scores.put(category, scores.get(category) + score);
            }

        }

        // create category entries
        CategoryEntries assignedEntries = new CategoryEntries();
        for (String category : categories) {
            assignedEntries.add(new CategoryEntry(category, scores.get(category)));
        }

        return assignedEntries;

    }

}