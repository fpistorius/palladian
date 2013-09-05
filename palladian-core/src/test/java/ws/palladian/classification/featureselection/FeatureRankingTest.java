/**
 * Created on: 20.11.2012 08:33:41
 */
package ws.palladian.classification.featureselection;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.classification.Instance;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BasicFeatureVectorImpl;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.SparseFeature;

/**
 * <p>
 * Tests whether the {@link FeatureRanker} works correctly or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class FeatureRankingTest {
    private Collection<Trainable> fixture;

    @Before
    public void setUp() {
        BasicFeatureVectorImpl fv1 = new BasicFeatureVectorImpl();
        BasicFeatureVectorImpl fv2 = new BasicFeatureVectorImpl();
        BasicFeatureVectorImpl fv3 = new BasicFeatureVectorImpl();
        BasicFeatureVectorImpl fv4 = new BasicFeatureVectorImpl();
        BasicFeatureVectorImpl fv5 = new BasicFeatureVectorImpl();

        ListFeature<SparseFeature<String>> listFeature1 = new ListFeature<SparseFeature<String>>("testfeature");
        listFeature1.add(new SparseFeature<String>("a"));
        listFeature1.add(new SparseFeature<String>("b"));
        listFeature1.add(new SparseFeature<String>("c"));
        listFeature1.add(new SparseFeature<String>("a"));
        listFeature1.add(new SparseFeature<String>("d"));
        fv1.add(listFeature1);

        ListFeature<SparseFeature<String>> listFeature2 = new ListFeature<SparseFeature<String>>("testfeature");
        listFeature2.add(new SparseFeature<String>("a"));
        listFeature2.add(new SparseFeature<String>("b"));
        listFeature2.add(new SparseFeature<String>("c"));
        fv2.add(listFeature2);

        ListFeature<SparseFeature<String>> listFeature3 = new ListFeature<SparseFeature<String>>("testfeature");
        listFeature3.add(new SparseFeature<String>("d"));
        listFeature3.add(new SparseFeature<String>("e"));
        listFeature3.add(new SparseFeature<String>("f"));
        fv3.add(listFeature3);
        
        ListFeature<SparseFeature<String>> listFeature4 = new ListFeature<SparseFeature<String>>("testfeature");
        listFeature4.add(new SparseFeature<String>("d"));
        listFeature4.add(new SparseFeature<String>("f"));
        fv4.add(listFeature4);
        
        ListFeature<SparseFeature<String>> listFeature5 = new ListFeature<SparseFeature<String>>("testfeature");
        listFeature5.add(new SparseFeature<String>("a"));
        listFeature5.add(new SparseFeature<String>("c"));
        fv5.add(listFeature5);

        Instance instance1 = new Instance("c1", fv1);
        Instance instance2 = new Instance("c1", fv2);
        Instance instance3 = new Instance("c2", fv3);
        Instance instance4 = new Instance("c2", fv4);
        Instance instance5 = new Instance("c1", fv5);

        fixture = new ArrayList<Trainable>(3);
        fixture.add(instance1);
        fixture.add(instance2);
        fixture.add(instance3);
        fixture.add(instance4);
        fixture.add(instance5);
    }

    @After
    public void tearDown() {
        fixture = null;
    }

    @Test
    public void testChiSquareFeatureSelection() {
        FeatureRanker featureSelector = new ChiSquaredFeatureRanker(new AverageMergingStrategy());

        FeatureRanking ranking = featureSelector.rankFeatures(fixture);
//         System.out.println(ranking);

        assertThat(ranking.getAll().get(5).getValue(), is("e"));
        assertThat(ranking.getAll().get(5).getScore(), is(closeTo(1.875, 0.0001)));
        assertThat(ranking.getAll().get(4).getValue(), is("b"));
        assertThat(ranking.getAll().get(4).getScore(), is(closeTo(2.22222, 0.0001)));
        assertThat(ranking.getAll().get(3).getValue(), is("d"));
        assertThat(ranking.getAll().get(3).getScore(), is(closeTo(2.22222, 0.0001)));
        assertThat(ranking.getAll().get(2).getValue(), is("a"));
        assertThat(ranking.getAll().get(2).getScore(), is(closeTo(5.0, 0.0001)));
        assertThat(ranking.getAll().get(1).getValue(), is("c"));
        assertThat(ranking.getAll().get(1).getScore(), is(closeTo(5.0, 0.0001)));
        assertThat(ranking.getAll().get(0).getValue(), is("f"));
        assertThat(ranking.getAll().get(0).getScore(), is(closeTo(5.0, 0.0001)));
    }

    @Test
    public void testChiSquaredRoundRobinMerge() throws Exception {
        FeatureRanker featureSelector = new ChiSquaredFeatureRanker(new RoundRobinMergingStrategy());

        FeatureRanking ranking = featureSelector.rankFeatures(fixture);
//         System.out.println(ranking);

        assertThat(ranking.getAll().get(5).getValue(), is("e"));
        assertThat(ranking.getAll().get(5).getScore(), is(closeTo(1.0, 0.0001)));
        assertThat(ranking.getAll().get(4).getValue(), is("b"));
        assertThat(ranking.getAll().get(4).getScore(), is(closeTo(2.0, 0.0001)));
        assertThat(ranking.getAll().get(3).getValue(), is("d"));
        assertThat(ranking.getAll().get(3).getScore(), is(closeTo(3.0, 0.0001)));
        assertThat(ranking.getAll().get(2).getValue(), is("a"));
        assertThat(ranking.getAll().get(2).getScore(), is(closeTo(4.0, 0.0001)));
        assertThat(ranking.getAll().get(1).getValue(), is("c"));
        assertThat(ranking.getAll().get(1).getScore(), is(closeTo(5.0, 0.0001)));
        assertThat(ranking.getAll().get(0).getValue(), is("f"));
        assertThat(ranking.getAll().get(0).getScore(), is(closeTo(6.0, 0.0001)));
    }

    @Test
    public void testInformationGain() throws Exception {
        FeatureRanker featureSelector = new InformationGainFeatureRanker();

        FeatureRanking ranking = featureSelector.rankFeatures(fixture);
        // System.out.println(ranking);

        assertThat(ranking.getAll().get(5).getValue(), is("e"));
        assertThat(ranking.getAll().get(5).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(4).getValue(), isOneOf("b", "d"));
        assertThat(ranking.getAll().get(4).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(3).getValue(), isOneOf("b", "d"));
        assertThat(ranking.getAll().get(3).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(2).getValue(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(2).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(1).getValue(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(1).getScore(), is(notNullValue()));
        assertThat(ranking.getAll().get(0).getValue(), isOneOf("a", "c", "f"));
        assertThat(ranking.getAll().get(0).getScore(), is(notNullValue()));
    }

    @Test
    public void testNumericFeatureWithInformationGain() throws Exception {
        List<Trainable> dataset = new ArrayList<Trainable>();
        BasicFeatureVectorImpl fV1 = new BasicFeatureVectorImpl();
        fV1.add(new NumericFeature("numeric", 1.0d));
        Instance instance1 = new Instance("a", fV1);
        dataset.add(instance1);
        BasicFeatureVectorImpl fV2 = new BasicFeatureVectorImpl();
        fV2.add(new NumericFeature("numeric", 2.0d));
        Instance instance2 = new Instance("b", fV2);
        dataset.add(instance2);
        BasicFeatureVectorImpl fV3 = new BasicFeatureVectorImpl();
        fV3.add(new NumericFeature("numeric", 3.0d));
        Instance instance3 = new Instance("a", fV3);
        dataset.add(instance3);

        FeatureRanker featureSelector = new InformationGainFeatureRanker();

        FeatureRanking ranking = featureSelector.rankFeatures(dataset);
        // System.out.println(ranking);

        assertThat(ranking.getAll().get(0).getValue(), is("numeric"));
        assertThat(ranking.getAll().get(0).getIdentifier(), is("feature"));
        assertThat(ranking.getAll().get(0).getScore(), is(notNullValue()));
    }
}