package ws.palladian.extraction.token;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotationFactory;

/**
 * <p>
 * A {@link BaseTokenizer} implementation based on regular expressions. Tokens are matched against the specified regular
 * expressions.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.1.7
 */
public final class RegExTokenizer extends BaseTokenizer {

    /**
     * <p>
     * The pattern that needs to match for a token to be extracted as a new {@code Annotation}.
     * </p>
     */
    private final Pattern pattern;

    private final String featureName;

    /**
     * <p>
     * The no argument constructor using {@link Tokenizer#SPLIT_PATTERN} to annotate token and saving them as
     * {@link Feature} with the identifier {@link BaseTokenizer#PROVIDED_FEATURE}.
     * </p>
     * 
     */
    public RegExTokenizer() {
        this(PROVIDED_FEATURE, Tokenizer.SPLIT_PATTERN);
    }

    /**
     * <p>
     * Creates a new {@code RegExTokenizer} creating token {@code Annotation}s with the provided identifier and
     * annotating token matching the provided {@code pattern}.
     * </p>
     * 
     * @param featureDescriptor The {@code FeatureDescriptor} identifying the annotated token.
     * @param pattern The pattern that needs to match for a token to be extracted as a new {@code Annotation}.
     */
    public RegExTokenizer(String featureName, Pattern pattern) {
        Validate.notNull(featureName, "featureName must not be null");
        Validate.notNull(pattern, "pattern must not be null");

        this.pattern = pattern;
        this.featureName = featureName;
    }

    @Override
    public void processDocument(TextDocument document) {
        Validate.notNull(document, "document must not be null");

        String text = document.getContent();
        FeatureVector featureVector = document.getFeatureVector();
        Matcher matcher = pattern.matcher(text);
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(featureName, document);
        while (matcher.find()) {
            featureVector.add(annotationFactory.create(matcher.start(), matcher.end()));
        }
    }

}
