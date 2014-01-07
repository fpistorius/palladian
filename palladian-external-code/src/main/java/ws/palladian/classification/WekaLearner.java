package ws.palladian.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BooleanFeature;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.SequentialPattern;
import ws.palladian.processing.features.SparseFeature;

/**
 * <p>
 * Learner wrapper for Weka classifiers.
 * </p>
 * 
 * @see <a href="http://www.cs.waikato.ac.nz/ml/weka/">Weka 3</a>
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 3.1
 * @since 0.1.7
 */
public final class WekaLearner implements Learner<WekaModel> {

    /**
     * This is necessary, because there is a bug when using sparse features in Weka ARFF files, therefore we need to add
     * one dummy attribute first. Says Klemens. See also Weka documentation, section 9.3 'Sparse ARFF files'.
     */
    static final String DUMMY_CLASS = "wekadummyclass";

    /** The prediction target, where the classification result goes. */
    private static final String TARGET_CLASS_ATTRIBUTE = "palladianWekaTargetClass";

    /** The Weka classifier this classifier wraps. */
    private final weka.classifiers.Classifier classifier;

    /**
     * <p>
     * Create a new {@link WekaLearner} with the specified Weka {@link Classifier} implementation.
     * </p>
     * 
     * @param classifier The classifier to use, not <code>null</code>.
     */
    public WekaLearner(weka.classifiers.Classifier classifier) {
        Validate.notNull(classifier, "classifier must not be null.");
        this.classifier = classifier;
    }

    @Override
    public WekaModel train(Iterable<? extends Trainable> trainables) {
        Validate.notNull(trainables);
        List<? extends Trainable> trainList = CollectionHelper.newArrayList(trainables);
        FastVector schema = new FastVector();
        Instances data = new Instances("dataset", schema, trainList.size());

        // Create schema for weka dataset.
        List<Map<Integer, Double>> wekaFeatureSets = new ArrayList<Map<Integer, Double>>(trainList.size());
        Set<String> classes = new HashSet<String>();
        List<String> instanceClasses = new ArrayList<String>(trainList.size());
        for (Trainable trainable : trainables) {
            Map<Integer, Double> wekaFeatureSet = new HashMap<Integer, Double>();
            for (Feature<?> feature : trainable.getFeatureVector()) {
                wekaFeatureSet.putAll(handleFeature(feature, data, trainables));
            }

            wekaFeatureSets.add(wekaFeatureSet);
            classes.add(trainable.getTargetClass());
            instanceClasses.add(trainable.getTargetClass());
        }

        // add attribute for the classification target
        FastVector targetClassVector = new FastVector();
        targetClassVector.addElement(DUMMY_CLASS);
        for (String targetClass : classes) {
            targetClassVector.addElement(targetClass);
        }
        Attribute classAttribute = new Attribute(TARGET_CLASS_ATTRIBUTE, targetClassVector);
        data.insertAttributeAt(classAttribute, data.numAttributes());

        // Add instances to weka dataset.
        int classIndex = data.numAttributes();
        for (int i = 0; i < wekaFeatureSets.size(); i++) {
            Map<Integer, Double> wekaFeatureSet = wekaFeatureSets.get(i);
            String targetClass = instanceClasses.get(i);
            int[] indices = new int[wekaFeatureSet.size() + 1];
            double[] values = new double[wekaFeatureSet.size() + 1];
            int j = 0;
            for (Map.Entry<Integer, Double> featureValue : wekaFeatureSet.entrySet()) {
                indices[j] = featureValue.getKey();
                values[j] = featureValue.getValue();
                j++;
            }
            indices[j] = classIndex - 1;
            values[j] = classAttribute.indexOfValue(targetClass);
            SparseInstance wekaInstance = new SparseInstance(1.0, values, indices, wekaFeatureSet.size());
            wekaInstance.setDataset(data);
            data.add(wekaInstance);
        }

        data.compactify();
        Attribute palladianWekaTargetClass = data.attribute(TARGET_CLASS_ATTRIBUTE);
        data.setClassIndex(palladianWekaTargetClass.index());
        try {
            classifier.buildClassifier(data);
        } catch (Exception e) {
            throw new IllegalStateException("An exception occurred while building the classifier: " + e.getMessage(), e);
        }
        return new WekaModel(classifier, data);
    }

    /**
     * <p>
     * Handles the conversion from a Palladian feature to a Weka attribute according to its type.
     * </p>
     * 
     * @param feature The Palladian feature to handle.
     * @param data The current Weka model, the feature should be added to.
     * @param trainables The Palladian training set containing the feature to convert.
     * @return A {@link Map} containing indices and values of all the feature and its possible subfeatures if it was a
     *         {@link ListFeature} within the Weka dataset.
     */
    private Map<Integer, Double> handleFeature(Feature<?> feature, Instances data,
            Iterable<? extends Trainable> trainables) {
        Map<Integer, Double> ret = new HashMap<Integer, Double>();

        if (feature instanceof ListFeature) {
            ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)feature;
            for (Feature<?> sparseFeature : listFeature) {
                // this is the magic!!!
                ret.putAll(handleRecursive(listFeature.getName() + sparseFeature.getName(), sparseFeature, data,
                        trainables));
            }
        } else {
            ret.putAll(handleRecursive(feature.getName(), feature, data, trainables));
        }

        return ret;
    }

    /**
     * <p>
     * Handles occurrences of {@link ListFeature}s recursivly and also contains the base code to convert the leaves,
     * which are also the features on the first level.
     * </p>
     * 
     * @param effectiveFeatureName The fully qualified name of the feature to convert. For basic features this is just
     *            the features name. For embedded features in a {@link ListFeature} this is the list features name plus
     *            the embedded features name.
     * @param feature The {@link Feature} to convert.
     * @param data The current state of the Weka model.
     * @param trainables The Palladian training set to build the dataset on.
     * @return A {@link Map} containing indices and values of all the feature and its possible embedded features if it
     *         was a {@link ListFeature} within the Weka dataset.
     */
    private Map<Integer, Double> handleRecursive(final String effectiveFeatureName, final Feature<?> feature,
            final Instances data, final Iterable<? extends Trainable> trainables) {
        Map<Integer, Double> ret = new HashMap<Integer, Double>();

        if (feature instanceof NominalFeature) {
            Attribute featureAttribute = data.attribute(effectiveFeatureName);
            if (featureAttribute == null) {
                // TODO It is not possible to use embedded NominalValues at the moment since this does not work with
                // assembled fully qualified 'effectiveFeatureName's.
                FastVector possibleValues = getValues(effectiveFeatureName, trainables);
                featureAttribute = new Attribute(effectiveFeatureName, possibleValues);
                data.insertAttributeAt(featureAttribute, data.numAttributes());
                featureAttribute = data.attribute(effectiveFeatureName);

            }

            Double featureValue = Integer.valueOf(featureAttribute.indexOfValue(feature.getValue().toString()))
                    .doubleValue();
            ret.put(featureAttribute.index(), featureValue);
        } else if (feature instanceof BooleanFeature) {
            Attribute featureAttribute = data.attribute(effectiveFeatureName);
            if (featureAttribute == null) {
                FastVector booleanValues = new FastVector(2);
                booleanValues.addElement("true");
                booleanValues.addElement("false");
                featureAttribute = new Attribute(effectiveFeatureName, booleanValues);
                data.insertAttributeAt(featureAttribute, data.numAttributes());
                featureAttribute = data.attribute(effectiveFeatureName);
            }

            Double featureValue = Integer.valueOf(featureAttribute.indexOfValue(feature.getValue().toString()))
                    .doubleValue();
            ret.put(featureAttribute.index(), featureValue);
        } else if (feature instanceof ListFeature) {
            ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)feature;
            for (Feature<?> sparseFeature : listFeature) {
                // this is magic as well!!!
                ret.putAll(handleRecursive(listFeature.getName() + feature.getName(), sparseFeature, data, trainables));
            }
        } else if (feature instanceof SparseFeature || feature instanceof PositionAnnotation
                || feature instanceof SequentialPattern) {
            Attribute featureAttribute = data.attribute(effectiveFeatureName);

            if (featureAttribute == null) {
                featureAttribute = new Attribute(effectiveFeatureName);
                data.insertAttributeAt(featureAttribute, data.numAttributes());
                featureAttribute = data.attribute(effectiveFeatureName);
            }
            ret.put(featureAttribute.index(), 1.0);
        } else {
            Attribute featureAttribute = data.attribute(feature.getName());
            if (featureAttribute == null) {
                featureAttribute = new Attribute(feature.getName());
                data.insertAttributeAt(featureAttribute, data.numAttributes());
                featureAttribute = data.attribute(feature.getName());
            }
            Double featureValue = Double.valueOf(feature.getValue().toString());
            ret.put(featureAttribute.index(), featureValue);
        }

        return ret;
    }

    // get domain for nominal feature, i.e. possible values
    /**
     * <p>
     * Extracts all possible values for a {@link NominalFeature}. This is necessary for Weka to know the domain and thus
     * declare all valid values within its schema.
     * </p>
     * 
     * @param name The name of the {@link NominalFeature} to create the domain for.
     * @param trainables The instances to use to create the domain.
     * @return A {@link FastVector} containing all values.
     */
    private FastVector getValues(String name, Iterable<? extends Trainable> trainables) {
        Set<String> nominalValues = new HashSet<String>();
        for (Trainable instance : trainables) {
            NominalFeature feature = instance.getFeatureVector().get(NominalFeature.class, name);
            if (feature == null) {
                continue;
            }
            nominalValues.add(feature.getValue());
        }
        FastVector fvNominalValues = new FastVector(nominalValues.size());
        for (String nominalValue : nominalValues) {
            fvNominalValues.addElement(nominalValue);
        }
        return fvNominalValues;
    }

    @Override
    public String toString() {
        return classifier.toString();
    }

}