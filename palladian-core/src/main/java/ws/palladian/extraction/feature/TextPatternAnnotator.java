package ws.palladian.extraction.feature;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.ProcessingPipeline;

public class TextPatternAnnotator extends AbstractDefaultPipelineProcessor {

    /**
	 * 
	 */
    private static final long serialVersionUID = 7064541848435617212L;

    public static final String PROVIDED_FEATURE = "TextAnnotatorFeature";

    public static final String EMAIL_PATTERN = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";

    private Pattern pattern;

    public TextPatternAnnotator(String pattern) {
        this(Pattern.compile(pattern));
    }

    public TextPatternAnnotator(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) {
        String content = document.getContent();
        Matcher matcher = pattern.matcher(content);
        AnnotationFeature feature = new AnnotationFeature(PROVIDED_FEATURE);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            Annotation annotation = new PositionAnnotation(document, start, end);
            feature.add(annotation);
        }
        document.getFeatureVector().add(feature);
    }

    public static void main(String[] args) throws DocumentUnprocessableException {

        PipelineDocument<String> document = new PipelineDocument<String>(
                "the quick brown fox jumps over the lazy dog. philipp@philippkatz.de");
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new TextPatternAnnotator(EMAIL_PATTERN));
        pipeline.process(document);

        AnnotationFeature feature = (AnnotationFeature)document.getFeatureVector().get(PROVIDED_FEATURE);
        System.out.println(feature.toStringList());

    }

}
