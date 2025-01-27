package ws.palladian.classification.numeric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.MinMaxNormalizer;
import ws.palladian.classification.utils.NoNormalizer;
import ws.palladian.classification.utils.ZScoreNormalizer;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.split.RandomSplit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * <p>
 * Tests for the numerical KNN classifier.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class KnnClassifierTest {

    /**
     * <p>
     * Tests the typical in memory usage of the Knn classifier. It is trained with three instances and tried out on one
     * {@link FeatureVector}. In the end the top class and its absolute relevance need to be correct.
     * </p>
     */
    @Test
    public void testKnnClassifier() {
        // create some instances for the vector space
        List<Instance> trainingInstances = new ArrayList<>();
        trainingInstances.add(new InstanceBuilder().set("f1", 3d).set("f2", 4d).set("f3", 5d).create("A"));
        trainingInstances.add(new InstanceBuilder().set("f1", 3d).set("f2", 6d).set("f3", 6d).create("A"));
        trainingInstances.add(new InstanceBuilder().set("f1", 4d).set("f2", 4d).set("f3", 4d).create("B"));

        // create the KNN classifier and add the training instances
        KnnLearner knnLearner = new KnnLearner(new NoNormalizer());
        KnnModel model = knnLearner.train(trainingInstances);
        FeatureVector featureVector = new InstanceBuilder().set("f1", 1d).set("f2", 2d).set("f3", 3d).create();

        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("A"));
        assertTrue(model.getCategories().contains("B"));

        // classify
        CategoryEntries result = new KnnClassifier(3).classify(featureVector, model);
        // assertEquals(0.6396, result.getProbability(result.getMostLikelyCategory()), 0.001);
        assertEquals("A", result.getMostLikelyCategory());
    }

    @Test
    public void testKnnClassifierSerialization() throws Exception {
        // create the KNN classifier and add the training instances
        KnnLearner knnLearner = new KnnLearner();
        Dataset instances = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/wineData.csv")).create();
        KnnModel model = knnLearner.train(instances);
        File tempDir = FileHelper.getTempDir();
        String tempFile = new File(tempDir, "/testKNN.gz").getPath();
        FileHelper.serialize(model, tempFile);
        KnnModel loadedModel = FileHelper.deserialize(tempFile);

        // classify
        CategoryEntries result = new KnnClassifier(3).classify(createTestInstance(), loadedModel);
        assertEquals(1, result.getProbability(result.getMostLikelyCategory()), 0);
        assertEquals("1", result.getMostLikelyCategory());
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
    	Dataset instances = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/adultData.txt")).readHeader(false).create();
    	RandomSplit split = new RandomSplit(instances, 0.5, new Random(123));
        KnnLearner learner = new KnnLearner(new NoNormalizer());
        ConfusionMatrix confusionMatrix = new ConfusionMatrixEvaluator().evaluate(learner, new KnnClassifier(3), split.getTrain(),split.getTest());
        // System.out.println(confusionMatrix.getAccuracy());
        assertTrue(confusionMatrix.getAccuracy() > 0.7);

        learner = new KnnLearner(new MinMaxNormalizer());
        confusionMatrix = new ConfusionMatrixEvaluator().evaluate(learner, new KnnClassifier(3), split.getTrain(),split.getTest());
        // System.out.println(confusionMatrix.getAccuracy());
        assertTrue(confusionMatrix.getAccuracy() > 0.75);

        learner = new KnnLearner(new ZScoreNormalizer());
        confusionMatrix = new ConfusionMatrixEvaluator().evaluate(learner, new KnnClassifier(3), split.getTrain(),split.getTest());
        // System.out.println(confusionMatrix.getAccuracy());
        assertTrue(confusionMatrix.getAccuracy() > 0.76);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
    	Dataset instances = CsvDatasetReaderConfig.filePath(getResourceFile("/classifier/diabetesData.txt")).readHeader(false).create();
    	RandomSplit split = new RandomSplit(instances, 0.5, new Random(123));
    	
    	ConfusionMatrix confusionMatrix = new ConfusionMatrixEvaluator().evaluate(new KnnLearner(new NoNormalizer()), new KnnClassifier(3), split.getTrain(),split.getTest());
    	// System.out.println(confusionMatrix.getAccuracy());
        assertTrue(confusionMatrix.getAccuracy() > 0.72);

        confusionMatrix = new ConfusionMatrixEvaluator().evaluate(new KnnLearner(new MinMaxNormalizer()), new KnnClassifier(3), split.getTrain(),split.getTest());
        // System.out.println(confusionMatrix.getAccuracy());
        assertTrue(confusionMatrix.getAccuracy() > 0.72);

        confusionMatrix = new ConfusionMatrixEvaluator().evaluate(new KnnLearner(new ZScoreNormalizer()), new KnnClassifier(3), split.getTrain(),split.getTest());
        // System.out.println(confusionMatrix.getAccuracy());
        assertTrue(confusionMatrix.getAccuracy() > 0.72);
    }

    private FeatureVector createTestInstance() {
        // create an instance to classify
        // 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 =>
        // this is an actual instance from the
        // training data and should therefore also be classified as "1"
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        instanceBuilder.set("alcohol", 13.82);
        instanceBuilder.set("malicAcid", 1.75);
        instanceBuilder.set("ash", 2.42);
        instanceBuilder.set("alcalinityOfAsh", 14d);
        instanceBuilder.set("magnesium", 111d);
        instanceBuilder.set("totalPhenols", 3.88);
        instanceBuilder.set("flavonoids", 3.74);
        instanceBuilder.set("nonflavonoidPhenols", .32);
        instanceBuilder.set("proanthocyanins", 1.87);
        instanceBuilder.set("colorIntensity", 7.05);
        instanceBuilder.set("hue", 1.01);
        instanceBuilder.set("od280/od315ofDilutedWines", 3.26);
        instanceBuilder.set("proline", 1190d);
        return instanceBuilder.create();
    }

}
