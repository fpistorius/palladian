package ws.palladian.retrieval;

import org.apache.commons.configuration.Configuration;
import org.w3c.dom.Document;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Download content from a different IP via a cloud.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://proxycrawl.com/docs/crawling-api/#your-first-api-call">Proxy Crawl API Docs</a>
 * 18.05.2021
 */
public class ProxyCrawlDocumentRetriever extends WebDocumentRetriever {
    private final String apiKeyPlain;
    private final String apiKeyJs;

    private boolean useJsRendering = false;

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_TOKEN_PLAIN = "api.proxycrawl.tokenplain";
    public static final String CONFIG_TOKEN_JS = "api.proxycrawl.tokenjs";

    /**
     * ProxyCrawl allows 20 requests/second.
     */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.SECONDS, 20);

    private final DocumentRetriever documentRetriever = new DocumentRetriever();

    public ProxyCrawlDocumentRetriever(Configuration configuration) {
        this.apiKeyPlain = configuration.getString(CONFIG_TOKEN_PLAIN);
        this.apiKeyJs = configuration.getString(CONFIG_TOKEN_JS);
    }

    public boolean isUseJsRendering() {
        return useJsRendering;
    }

    public void setUseJsRendering(boolean useJsRendering) {
        this.useJsRendering = useJsRendering;
    }

    @Override
    public Document getWebDocument(String url) {
        THROTTLE.hold();
        String requestUrl = "https://api.proxycrawl.com/?token=" + getActiveToken() + "&url=" + UrlHelper.encodeParameter(url);
        Document d = documentRetriever.getWebDocument(requestUrl);
        if (d != null) {
            d.setDocumentURI(url);
        }
        return d;
    }

    private String getActiveToken() {
        return isUseJsRendering() ? apiKeyJs : apiKeyPlain;
    }
}