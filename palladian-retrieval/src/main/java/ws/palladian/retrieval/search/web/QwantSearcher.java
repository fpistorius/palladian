package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Using the unofficial Qwant API without limits.
 * </p>
 * 
 * @author David Urbansky
 * @since 18.05.2019
 */
public final class QwantSearcher extends AbstractMultifacetSearcher<WebContent> {
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(QwantSearcher.class);

    /** The name of this WebSearcher. */
    private static final String SEARCHER_NAME = "Qwant";

    private final HttpRetriever retriever;

    /**
     * <p>
     * Creates a new Qwant Searcher.
     * </p>
     *
     */
    public QwantSearcher() {
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {
        List<WebContent> results = new ArrayList<>();
        Long resultCount = null;

        // Qwant gives chunks of max. 10 items, and allows 10 chunks, i.e. max. 100 results.
        double numChunks = Math.min(10, Math.ceil((double)query.getResultCount() / 10));

        for (int start = 1; start <= numChunks; start++) {
            String searchUrl = createRequestUrl(query.getText(), start, Math.min(10, query.getResultCount() - results.size()), query.getLanguage());
            LOGGER.debug("Search with URL " + searchUrl);

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(searchUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP exception while accessing " + getName() + " with URL \"" + searchUrl + "\": " + e.getMessage(), e);
            }

            String jsonString = httpResult.getStringContent();
            if (StringUtils.isBlank(jsonString)) {
                throw new SearcherException("JSON response is empty.");
            }
            JsonObject responseJson;
            try {
                responseJson = new JsonObject(jsonString);
                checkError(responseJson);
                List<WebContent> current = parse(responseJson);
                if (current.isEmpty()) {
                    break;
                }
                results.addAll(current);
                if (resultCount == null) {
                    resultCount = parseResultCount(responseJson);
                }
            } catch (JsonException e) {
                throw new SearcherException("Error parsing the response from URL \"" + searchUrl + "\" (JSON was: \"" + jsonString + "\"): " + e.getMessage(), e);
            }
        }

        return new SearchResults<>(results, resultCount);
    }

    private String createRequestUrl(String query, int offset, int num, Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://api.qwant.com/api/search/web");
        urlBuilder.append("?count=").append(num);
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&q=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&t=web");
        urlBuilder.append("&safesearch=0");
        urlBuilder.append("&uiv=4");
        if (language != null) {
            urlBuilder.append("&locale=").append(getLanguageCode(language));
        }
        return urlBuilder.toString();
    }

    /**
     * @see https://github.com/NLDev/qwant-api/blob/master/lib/langs.js
     * @param language Palladian language.
     * @return Language identifier
     */
    private String getLanguageCode(Language language) {
        switch (language) {
            case BULGARIAN:
                return "bg_bg";
            case CATALAN:
                return "ca_ca";
            case CZECH:
                return "cs_cs";
            case DANISH:
                return "da_da";
            case GERMAN:
                return "de_de";
            case GREEK:
                return "el_el";
            case ENGLISH:
                return "en_us";
            case SPANISH:
                return "es_es";
            case ESTONIAN:
                return "et_et";
            case FINNISH:
                return "fi_fi";
            case FRENCH:
                return "fr_fr";
            case HUNGARIAN:
                return "hu_hu";
            case ITALIAN:
                return "it_it";
            case JAPANESE:
                return "ja_ja";
            case MALAY:
                return "ms_ms";
            case HEBREW:
                return "he_he";
            case KOREAN:
                return "ko_ko";
            case THAI:
                return "th_th";
            case DUTCH:
                return "nl_nl";
            case NORWEGIAN:
                return "no_no";
            case POLISH:
                return "pl_pl";
            case PORTUGUESE:
                return "pt_pt";
            case ROMANIAN:
                return "ro_ro";
            case RUSSIAN:
                return "ru_ru";
            case SWEDISH:
                return "sv_sv";
            case TURKISH:
                return "tr_tr";
            case WELSH:
                return "cy_cy";
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    static void checkError(JsonObject jsonObject) throws SearcherException {
        Integer errorCode = jsonObject.tryQueryInt("data/error_code");
        if (errorCode != null) {
            throw new SearcherException("Error from Qwant API: " + errorCode);
        }
    }

    /** default visibility for unit testing. */
    static List<WebContent> parse(JsonObject jsonObject) {
        JsonArray jsonItems = jsonObject.tryQueryJsonArray("data/result/items");
        if (jsonItems == null) {
            LOGGER.warn("JSON result did not contain an 'items' property. (JSON = '" + jsonObject.toString() + "'.");
            return Collections.emptyList();
        }
        List<WebContent> result = new ArrayList<>();
        for (int i = 0; i < jsonItems.size(); i++) {
            JsonObject jsonItem = jsonItems.tryGetJsonObject(i);
            BasicWebContent.Builder builder = new BasicWebContent.Builder();
            builder.setTitle(jsonItem.tryGetString("title"));
            builder.setUrl(jsonItem.tryGetString("url"));
            builder.setSummary(jsonItem.tryGetString("desc"));
            builder.setSource(SEARCHER_NAME);
            result.add(builder.create());
        }
        return result;
    }

    static long parseResultCount(JsonObject jsonObject) {
        return jsonObject.tryQueryInt("data/result/total");
    }
}
