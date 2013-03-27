/**
 * Created on: 14.12.2012 08:10:45
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.apache.commons.lang.Validate;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureUtils;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A wrapper classifier for the LIBSVM machine learning library.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 2.0
 */
public final class LibSvmPredictor implements Classifier<LibSvmModel> {
    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LibSvmPredictor.class);

    private final List<String> normalFeaturePaths;
    private final List<String> sparseFeaturePaths;
    private final LibSvmKernel kernel;

    private Map<NominalFeature, List<String>> possibleNominalValues;
    private int currentIndex;
    /**
     * <p>
     * The training instances provided to the train method, stored here for convenience.
     * </p>
     */
    private List<Instance> instances;

    /**
     * <p>
     * Creates a new completely initialized {@link LibSvmPredictor} using a linear kernel. It can be used to either
     * train a new model or classify unlabeled {@link FeatureVector}s.
     * </p>
     * 
     * @param regularizationParameter The regularization parameter (in (0,infinity]); large values cause a low bias,
     *            high variance classifier, while small ones cause high bias, low variance. The correct value can be
     *            determined for example via grid search. This parameter is ignored for classification since the model
     *            provides the value in this case. The parameter is often called "C" within the SVM optimization
     *            formula.
     * @param normalFeaturePaths The feature paths identifying the normal features, which should be considered for
     *            training. This parameter is ignored for classification since the model provides the value in this
     *            case.
     * @param sparseFeaturePaths The feature paths identifying the sparse features, which should be considered for
     *            training. This parameter is ignored for classification since the model provides the value in this
     *            case.
     */
    public LibSvmPredictor(LibSvmKernel kernel, List<String> normalFeaturePaths, List<String> sparseFeaturePaths) {
        super();

        this.normalFeaturePaths = new ArrayList<String>(normalFeaturePaths);
        this.sparseFeaturePaths = new ArrayList<String>(sparseFeaturePaths);
        this.kernel = kernel;
    }

    @Override
    public LibSvmModel train(List<Instance> instances) {
        Validate.notEmpty(instances, "Unable to train on an empty list of instances.");

        svm_parameter params = getParameter();

        Map<String, Integer> indices = new HashMap<String, Integer>();
        List<String> classes = calculatePossibleClasses(instances);
        Map<String, Normalization> normalizations = normalizeNumericFeatures(instances);
        svm_problem problem = createProblem(instances, params, indices, classes, normalizations);
        String errorMessage = svm.svm_check_parameter(problem, params);
        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        String error_msg = svm.svm_check_parameter(problem, params);

        if (error_msg != null) {
            throw new IllegalStateException(error_msg);
        }
        svm_model model = svm.svm_train(problem, params);

        return new LibSvmModel(model, normalFeaturePaths, sparseFeaturePaths, indices, classes, normalizations);
    }

    private Map<String, Normalization> normalizeNumericFeatures(List<Instance> instances) {
        Map<String, Normalization> ret = new HashMap<String, Normalization>();
        for (Instance instance : instances) {
            for (String featurePath : normalFeaturePaths) {
                List<NumericFeature> features = FeatureUtils.getFeaturesAtPath(instance.getFeatureVector(),
                        NumericFeature.class, featurePath);

                // since it is a normal feature it should have exactly one element, if not it was no NumericFeature and
                // thus the iterations should continue with the next feature.
                if (features.size() != 1) {
                    continue;
                }

                NumericFeature numericFeature = features.get(0);
                Normalization normalization = ret.get(featurePath);
                if (normalization == null) {
                    normalization = new Normalization();
                }
                normalization.add(numericFeature);
                ret.put(featurePath, normalization);
            }
        }
        return ret;
    }

    /**
     * <p>
     * Transforms the set of Palladian {@link Instance}s to a libsvm {@link svm_problem}, which is the input to train a
     * libsvm classifier.
     * </p>
     * 
     * @param instances The Palladian instances to transform.
     * @param params The parameters for the classifier. Required to set parameter which are based on the training set.
     * @param indices The indices of the features to process in the new model.
     * @param classes The possible classes to predict to. The index in the list is the index used to convert those
     *            classes to numbers.
     * @param normalizations The normalizations to apply to {@link NumericFeature}s.
     * @return A new {@link svm_problem} ready to train a libsvm classifier.
     */
    private svm_problem createProblem(List<Instance> instances, svm_parameter params, Map<String, Integer> indices,
            List<String> classes, Map<String, Normalization> normalizations) {

        svm_problem ret = new svm_problem();
        this.instances = instances;
        ret.l = this.instances.size();
        ret.x = new svm_node[ret.l][];
        ret.y = new double[ret.l];
        currentIndex = 0;
        possibleNominalValues = new HashMap<NominalFeature, List<String>>();

        for (int i = 0; i < instances.size(); i++) {
            Instance instance = this.instances.get(i);
            ret.y[i] = classes.indexOf(instance.getTargetClass());

            ret.x[i] = transformPalladianFeatureVectorToLibsvmFeatureVector(instance.getFeatureVector(), indices, true,
                    normalizations);
        }
        // params.gamma = 1.0 / Collections.max(indices.values());

        return ret;
    }

    private <T extends Feature<?>> double featureToDouble(T feature, List<Instance> instances,
            Map<String, Normalization> normalizations) {
        if (feature instanceof NumericFeature) {
            NumericFeature numericFeature = (NumericFeature)feature;
            // TODO this will not work always, since the name is only the last part of the feature path. Should however
            // be possible if we provide a featureDescriptor carrying the whole path and not just the name.
            Normalization normalization = normalizations.get(numericFeature.getName());
            if (normalization != null) {
                return normalization.apply(numericFeature.getValue());
            } else {
                return numericFeature.getValue();
            }
        } else if (feature instanceof Classifiable) {
            return 1.0;
        } else if (feature instanceof NominalFeature) {
            List<String> values = getNominalValues(((NominalFeature)feature), instances);
            return values.indexOf(feature.getValue());
        } else {
            throw new IllegalArgumentException("Unsupported feature type " + feature.getClass());
        }

    }

    private List<String> getNominalValues(NominalFeature nominalFeature, List<Instance> instances) {
        if (possibleNominalValues.containsKey(nominalFeature)) {
            return possibleNominalValues.get(nominalFeature);
        } else {
            List<String> ret = new ArrayList<String>();
            for (Instance instance : instances) {
                @SuppressWarnings("unchecked")
                List<NominalFeature> features = (List<NominalFeature>)FeatureUtils.find(nominalFeature,
                        instance.getFeatureVector());
                for (NominalFeature feature : features) {
                    ret.add(feature.getValue());
                }
            }
            Collections.sort(ret);
            possibleNominalValues.put(nominalFeature, ret);
            return ret;
        }
    }

    /**
     * <p>
     * Provides a {@link List} of all possible classes from the provided {@link Instance}s.
     * </p>
     * 
     * @param instances The {@link Instance}s to load the classes for.
     * @return The {@link List} of classes supported by the {@link Instance}s.
     */
    private List<String> calculatePossibleClasses(List<Instance> instances) {
        List<String> ret = new ArrayList<String>();
        for (Instance instance : instances) {
            if (!ret.contains(instance.getTargetClass())) {
                ret.add(instance.getTargetClass());
            }
        }
        Collections.sort(ret);
        return ret;
    }

    /**
     * <p>
     * Transforms a Palladian {@link FeatureVector}
     * </p>
     * 
     * @param vector The {@link FeatureVector} to transform.
     * @param indices A {@link Map} filled with the correct indices for all the features if {@code trainingMode} is
     *            {@code true}.
     * @param trainingMode Tells the function whether to fill the indices {@link Map} or not. In training mode, those
     *            indices are required to build the libsvm dataset. This is a shortcut, so we don't have to iterate all
     *            the features twice.
     * @param normalizations A {@link Map} of normalizations that are applied to all {@link NumericFeature}s.
     * @return An array of {@link svm_node} instances.
     */
    private svm_node[] transformPalladianFeatureVectorToLibsvmFeatureVector(FeatureVector vector,
            Map<String, Integer> indices, boolean trainingMode, Map<String, Normalization> normalizations) {
        Map<String, Feature<?>> features = new HashMap<String, Feature<?>>();
        Map<String, Feature<?>> sparseFeatures = new HashMap<String, Feature<?>>();

        for (String featurePath : normalFeaturePaths) {
            List<Feature<?>> normalFeatures = FeatureUtils.getFeaturesAtPath(vector, featurePath);
            if (normalFeatures.size() != 1) {
                throw new IllegalStateException("Found " + normalFeatures.size() + " values for feature " + featurePath
                        + " but expected 1.");
            }
            features.put(normalFeatures.get(0).getName(), normalFeatures.get(0));
            if (trainingMode && !indices.containsKey(normalFeatures.get(0).getName())) {
                indices.put(normalFeatures.get(0).getName(), currentIndex++);
            }
        }

        for (String featurePath : sparseFeaturePaths) {
            List<Feature<?>> feature = FeatureUtils.getFeaturesAtPath(vector, featurePath);
            for (Feature<?> sparseFeature : feature) {
                sparseFeatures.put(sparseFeature.getValue().toString(), sparseFeature);

                if (trainingMode) {
                    if (!indices.containsKey(sparseFeature.getValue().toString())) {
                        indices.put(sparseFeature.getValue().toString(), currentIndex++);
                    }
                }
            }
        }
        List<svm_node> libSvmFeatureVector = new ArrayList<svm_node>();
        for (Entry<String, Feature<?>> entry : features.entrySet()) {
            svm_node node = new svm_node();
            node.index = indices.get(entry.getKey());
            node.value = featureToDouble(entry.getValue(), instances, normalizations);
            libSvmFeatureVector.add(node);
        }

        for (Entry<String, Feature<?>> entry : sparseFeatures.entrySet()) {
            Integer featureIndex = indices.get(entry.getKey());
            if (featureIndex == null) {
                LOGGER.debug("Ignoring sparse feature \"" + entry.getKey() + "\" since it was not in the training set.");
                continue;
            }
            svm_node node = new svm_node();
            node.index = featureIndex;
            node.value = 1.0;
            libSvmFeatureVector.add(node);
        }
        return libSvmFeatureVector.toArray(new svm_node[libSvmFeatureVector.size()]);
    }

    private svm_parameter getParameter() {
        svm_parameter ret = new svm_parameter();
        kernel.apply(ret);
        ret.svm_type = svm_parameter.C_SVC;
        ret.degree = 3;
        ret.coef0 = 0;
        ret.nu = 0.5;
        ret.cache_size = 100;
        ret.eps = 0.001;
        ret.p = 0.1;
        ret.shrinking = 1;
        ret.probability = 0;
        ret.nr_weight = 0;
        ret.weight_label = new int[0];
        ret.weight = new double[0];

        return ret;
    }

    @Override
    public LibSvmModel train(Dataset dataset) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CategoryEntries classify(FeatureVector vector, LibSvmModel model) {
        CategoryEntries ret = new CategoryEntries();

        svm_node[] libsvmFeatureVector = transformPalladianFeatureVectorToLibsvmFeatureVector(vector,
                model.getSchema(), false, model.getNormalizations());

        double classIndex = svm.svm_predict(model.getModel(), libsvmFeatureVector);
        String className = model.transformClassToString(Double.valueOf(classIndex).intValue());
        ret.add(new CategoryEntry(className, 1.0));

        return ret;
    }

    /**
     * <p>
     * Writes the {@link FeatureVector}s of the provided instances to disk using the LibSvm format.
     * </p>
     * 
     * @param instances The instances to get the {@link FeatureVector}s to write from.
     * @param targetFilePath The path to write the output to.
     */
    public void writeToDisk(List<Instance> instances, String targetFilePath) {
        Map<String, Normalization> normalizations = normalizeNumericFeatures(instances);
        Map<String, Integer> indices = new HashMap<String, Integer>();
        List<String> possibleClasses = calculatePossibleClasses(instances);
        for (Instance instance : instances) {
            StringBuilder out = new StringBuilder(String.valueOf(possibleClasses.indexOf(instance.getTargetClass())));
            svm_node[] nodes = transformPalladianFeatureVectorToLibsvmFeatureVector(instance.getFeatureVector(),
                    indices, true, normalizations);
            List<svm_node> sortedNodes = Arrays.asList(nodes);
            Collections.sort(sortedNodes, new Comparator<svm_node>() {

                @Override
                public int compare(svm_node o1, svm_node o2) {
                    return Integer.valueOf(o1.index).compareTo(o2.index);
                }
            });

            for (svm_node node : sortedNodes) {
                out.append(" ").append(node.index).append(":").append(node.value);
            }
            out.append("\n");

            String line = out.toString();
            FileHelper.appendFile(targetFilePath, line);
        }
    }

}
