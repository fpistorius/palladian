package ws.palladian.classification.sentiment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.core.Category;

public abstract class AbstractSentimentClassifier {
    /** Only sentences above this confidence threshold are taken into account for later calculations. */
    protected double confidenceThreshold = 0.5;

    /** Store a list of all opinionated sentences. */
    private Map<String, List<String>> opinionatedSentences = new HashMap<>();

    /**
     * <p>
     * Classify a text as rather positive or negative.
     * </p>
     * <p>
     * We simply look up the sentiment for each word, negate the sentiment if we find a "nicht" before the word, and
     * emphasize the sentiment if we find and emphasizing word such as "sehr".
     * </p>
     * 
     * @param text The text to be classified.
     * @return A CategoryEntry with the likelihood.
     */
    public Category getPolarity(String text) {
        opinionatedSentences = new HashMap<>();
        return getPolarity(text, null);
    }

    public abstract Category getPolarity(String text, String query);

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public Map<String, List<String>> getOpinionatedSentences() {
        return opinionatedSentences;
    }

    protected void addOpinionatedSentence(String sentiment, String opinionatedSentence) {
        List<String> sentimentSentences = this.opinionatedSentences.get(sentiment);

        if (sentimentSentences == null) {
            sentimentSentences = new ArrayList<>();
        }

        sentimentSentences.add(opinionatedSentence);

        opinionatedSentences.put(sentiment, sentimentSentences);
    }
}