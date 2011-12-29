package ws.palladian.retrieval.ranking.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * Base implementation for {@link RankingService}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class BaseRankingService implements RankingService {

    /** DocumentRetriever for HTTP downloading purposes. */
    protected final HttpRetriever retriever;

    public BaseRankingService() {
        retriever = HttpRetrieverFactory.getHttpRetriever();
        
        // we use a rather short timeout here, as responses are short.
        retriever.setConnectionTimeout(5000);
    }

    @Override
    public Map<String, Ranking> getRanking(List<String> urls) {
        Map<String, Ranking> results = new HashMap<String, Ranking>();
        if (!isBlocked()) {
            // iterate through urls and get ranking for each
            for (String url : urls) {
                results.put(url, getRanking(url));
            }
        }
        return results;
    }

    @Override
    public RankingType getRankingType(String id) {
        List<RankingType> rankingTypes = getRankingTypes();
        for (RankingType rankingType : rankingTypes) {
            if (rankingType.getId().equals(id)) {
                return rankingType;
            }
        }
        return null;
    }

    @Override
    public boolean checkBlocked() {
        return false;
    }

    @Override
    public boolean isBlocked() {
        return false;
    }

    @Override
    public void resetBlocked() {
    }

}
