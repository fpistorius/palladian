/**
 * Created on: 19.06.2012 20:01:30
 */
package ws.palladian.extraction.patterns;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.processing.features.SequentialPattern;

/**
 * <p>
 * An extraction strategy using NGrams.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
 */
public final class NGramPatternExtractionStrategy implements SpanExtractionStrategy {

    @Override
    public List<SequentialPattern> extract(String[] tokenList, Integer minPatternSize,
            Integer maxPatternSize) {
        List<SequentialPattern> ret = new ArrayList<SequentialPattern>();
        List<List<String>> patterns = Tokenizer.calculateAllNGrams(tokenList, minPatternSize, maxPatternSize);
        for (List<String> pattern : patterns) {
            ret.add(new SequentialPattern(pattern));
        }
        return ret;
    }

}
