package ws.palladian.classification.discretization;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.discretization.Binner.Interval;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

public final class Discretization {

    private final Map<String, Binner> binners = CollectionHelper.newHashMap();

    public Discretization(Iterable<? extends Instance> dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        Collection<Instance> datasetCopy = CollectionHelper.newArrayList(dataset);
        Set<String> numericFeatureNames = getNumericFeatureNames(datasetCopy);
        ProgressMonitor progressMonitor = new ProgressMonitor(numericFeatureNames.size());
        for (String featureName : numericFeatureNames) {
            binners.put(featureName, new Binner(datasetCopy, featureName));
            progressMonitor.incrementAndPrintProgress();
        }
    }

    /**
     * Get the names of all {@link NumericValue}s in the given dataset.
     * 
     * @param dataset The dataset.
     * @return Names of {@link NumericValue}s.
     * @deprecated Move this logic to the {@link DatasetStatistics} class.
     */
    @Deprecated
    private static Set<String> getNumericFeatureNames(Iterable<? extends Instance> dataset) {
        Set<String> numericFeatureNames = CollectionHelper.newHashSet();
        for (Instance instance : dataset) {
            FeatureVector featureVector = instance.getVector();
            for (VectorEntry<String, Value> vectorEntry : featureVector) {
                if (vectorEntry.value() instanceof NumericValue) {
                    numericFeatureNames.add(vectorEntry.key());
                }
            }
        }
        return numericFeatureNames;
    }

    public FeatureVector discretize(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        for (VectorEntry<String, Value> vectorEntry : featureVector) {
            String featureName = vectorEntry.key();
            Value featureValue = vectorEntry.value();
            if (featureValue instanceof NumericValue) {
                NumericValue numericValue = (NumericValue)featureValue;
                Binner binner = binners.get(featureName);
                Interval bin = binner.getBin(numericValue.getDouble());
                instanceBuilder.set(featureName, bin);
            } else {
                instanceBuilder.set(featureName, featureValue);
            }
        }
        return instanceBuilder.create();
    }
    
    public Binner getBinner(String featureName) {
        Validate.notEmpty(featureName, "featureName must not be empty");
        return binners.get(featureName);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Binner binner : binners.values()) {
            builder.append(binner + "\n");
        }
        return builder.toString();
    }

}
