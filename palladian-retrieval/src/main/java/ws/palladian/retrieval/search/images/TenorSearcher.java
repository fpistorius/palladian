package ws.palladian.retrieval.search.images;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for free gifs from <a href="https://tenor.com">Tenor</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="https://tenor.com/gifapi/documentation">Tenor API Docs</a>
 */
public class TenorSearcher extends AbstractSearcher<WebImage> {
    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Tenor";

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.tenor.key";

    private final String apiKey;

    /**
     * <p>
     * Creates a new Tenor searcher.
     * </p>
     *
     * @param apiKey The API key for accessing Tenor, not <code>null</code> or empty.
     */
    public TenorSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new Tenor searcher.
     * </p>
     *
     * @param configuration The configuration which must provide an API key for accessing Tenor, which must be
     *            provided as string via key {@value TenorSearcher#CONFIG_API_KEY} in the configuration.
     */
    public TenorSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    /**
     * @param language Supported languages are English.
     */
    public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImage> results = new ArrayList<>();

        resultCount = defaultResultCount == null ? resultCount : defaultResultCount;
        int resultsPerPage = Math.min(50, resultCount);

        DocumentRetriever documentRetriever = new DocumentRetriever();

        String requestUrl = buildRequest(query, resultsPerPage);
        try {
            JsonObject jsonResponse = documentRetriever.getJsonObject(requestUrl);
            if (jsonResponse == null) {
                throw new SearcherException("Failed to get JSON from " + requestUrl);
            }
            JsonObject json = new JsonObject(jsonResponse);
            JsonArray jsonArray = json.getJsonArray("results");
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject resultHit = jsonArray.getJsonObject(i);

                JsonObject media = resultHit.tryGetJsonArray("media").getJsonObject(0);
                BasicWebImage.Builder builder = new BasicWebImage.Builder();
                builder.setUrl(resultHit.getString("url"));
                builder.setImageUrl(media.tryQueryString("gif/url"));
                builder.setTitle(resultHit.tryGetString("title"));
                JsonArray dimensions = media.tryQueryJsonArray("gif/dims");
                builder.setWidth(dimensions.getInt(0));
                builder.setHeight(dimensions.getInt(1));
                builder.setImageType(ImageType.GIF);
                builder.setThumbnailUrl(media.tryQueryString("nanogif/url"));
                builder.setLicense(License.FREE);
                builder.setLicenseLink("https://tenor.com/gifapi/documentation#attribution");
                results.add(builder.create());
                if (results.size() >= resultCount) {
                    break;
                }
            }
        } catch (JsonException e) {
            throw new SearcherException(e.getMessage());
        }

        return results;
    }

    private String buildRequest(String searchTerms, int limit) {
        return String.format("https://api.tenor.com/v1/search?key=%s&q=%s&limit=%s", apiKey, UrlHelper.encodeParameter(searchTerms), limit);
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        TenorSearcher searcher = new TenorSearcher("KEY");
        List<WebImage> results = searcher.search("pizza", 101);
        CollectionHelper.print(results);
    }
}
