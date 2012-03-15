/**
 * Created on: 30.01.2012 15:22:30
 */
package ws.palladian.preprocessing.featureextraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.model.SequentialPattern;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.ProcessingPipeline;
import ws.palladian.preprocessing.nlp.pos.OpenNlpPosTagger;
import ws.palladian.preprocessing.nlp.sentencedetection.AbstractSentenceDetector;
import ws.palladian.preprocessing.nlp.sentencedetection.PalladianSentenceDetector;
import ws.palladian.preprocessing.nlp.tokenization.RegExTokenizer;

// TODO since the LSP algorithm is so slow this test should not run regularly.
/**
 * <p>
 * Tests whether the Labeled Sequential Pattern annotator works correct or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * 
 */
@RunWith(Parameterized.class)
@Ignore
public class SequentialPatternAnnotatorTest {

    private final String inputText;
    private final String[] keywords;
    private final List<SequentialPattern> expectedPatterns;

    public SequentialPatternAnnotatorTest(String inputText, String[] keywords, List<SequentialPattern> expectedPatterns) {
        super();
        this.inputText = inputText;
        this.keywords = keywords;
        this.expectedPatterns = expectedPatterns;
    }

    @Parameters
    public static Collection<Object[]> testData() throws IOException {
        List<SequentialPattern> firstExtractedPatterns = new ArrayList<SequentialPattern>();
        List<SequentialPattern> thirdExtractedPatterns = new ArrayList<SequentialPattern>();
        List<SequentialPattern> secondExtractedPatterns = new ArrayList<SequentialPattern>();

        List<String> firstPattern = new ArrayList<String>();
        List<String> secondPattern = new ArrayList<String>();
        List<String> thirdPattern = new ArrayList<String>();
        List<String> fourthPattern = new ArrayList<String>();
        List<String> fifthPattern = new ArrayList<String>();

        firstPattern.addAll(Arrays.asList(new String[] {"This", "VBZ", "DT", "test"}));
        secondPattern.addAll(Arrays.asList(new String[] {"RB", ",", "this", "VBZ", "the", "RBS", "JJ", "NN", "IN",
                "DT", "NNP", "NN", "PRP", "''", "JJ", "RB", "VBN", "."}));
        thirdPattern.addAll(Arrays.asList(new String[] {"PRP", "VBP", ":", "IN", "JJ", "NN", "VBZ", "DT", "NN", "NNP",
                "NN", ":", "IN", "PRP", "RB", "VB", "JJR", "IN", "DT", "NN", "RB", "IN", "PRP", "VBZ", "VBG", "WRB",
                "DT", "NNS", "VBP", "VBN", "."}));
        fourthPattern.addAll(Arrays.asList(new String[] {"PRP", "MD", "VB", "TO", "NN", "DT", "NNP", "NN", "CC", "RB",
                "VB", "PRP", "IN", "DT", "NN", "PRP", "VBP", "."}));
        fifthPattern.addAll(Arrays.asList(new String[] {"JJ", "NN", "VBZ", "DT", "JJS", "IN", "PRP$", "NNS", "RB",
                "RB", ":"}));

        firstExtractedPatterns.add(new SequentialPattern(firstPattern));
        secondExtractedPatterns.add(new SequentialPattern(secondPattern));
        secondExtractedPatterns.add(new SequentialPattern(thirdPattern));
        secondExtractedPatterns.add(new SequentialPattern(fourthPattern));
        thirdExtractedPatterns.add(new SequentialPattern(fifthPattern));

        return Arrays
                .asList(new Object[] {"This is a test", new String[] {"This", "test"}, firstExtractedPatterns},
                        new Object[] {
                                "honestly, this is the most bizarre reference t a JS library I've ever seen. I assume - if http access produces a valie JS file - that it just loads slower as the page so that it is missing when the objects are called. You may want to download the JS library and then link it from the application you build.",
                                new String[] {"this", "is", "the", "what,", "how", "who", "a"}, secondExtractedPatterns},
                        new Object[] {"Slow loading is the least of my problems right now... ", new String[] {},
                                thirdExtractedPatterns});
    }

    @Test
    public final void test() throws Exception {
        ProcessingPipeline processingPipeline = new ProcessingPipeline();
        processingPipeline.add(new PalladianSentenceDetector());
        processingPipeline.add(new RegExTokenizer());
        processingPipeline.add(new OpenNlpPosTagger(ResourceHelper
                .getResourceFile("/model/en-pos-maxent.bin")));
        processingPipeline.add(new SequentialPatternAnnotator(keywords, 4));

        PipelineDocument document = new PipelineDocument(inputText);

        processingPipeline.process(document);

        for (Annotation annotation : ((AnnotationFeature)document.getFeatureVector().get(
                AbstractSentenceDetector.PROVIDED_FEATURE)).getValue()) {
            SequentialPattern lsp = (SequentialPattern)annotation.getFeatureVector()
                    .get(SequentialPatternAnnotator.PROVIDED_FEATURE).getValue();
            Assert.assertThat(lsp, Matchers.isIn(expectedPatterns));
        }
    }
}