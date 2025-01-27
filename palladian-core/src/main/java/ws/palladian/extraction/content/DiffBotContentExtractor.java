package ws.palladian.extraction.content;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * The DiffBotContentExtractor extracts clean sentences from (English) texts.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://www.diffbot.com/our-apis/article/">Diffbot: Article API</a>
 */
public class DiffBotContentExtractor extends WebPageContentExtractor {
    /** The name of this extractor. */
    private static final String EXTRACTOR_NAME = "DiffBot Content Extractor";

    /** For performing HTTP requests. */
    private final HttpRetriever httpRetriever;

    /** The API key for accessing the service. */
    private final String apiKey;

    private String extractedResult = "";
    private String extractedTitle = "";
    private String extractedAuthor = "";
    private String extractedDate = "";

    public DiffBotContentExtractor(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

//    @Override
//    public WebPageContentExtractor setDocument(String documentLocation) throws PageContentExtractorException {
//        return setDocument(documentLocation, true);
//    }

    @Override
    public WebPageContentExtractor setDocument(String documentLocation, boolean parse) throws PageContentExtractorException {
        String requestUrl = buildRequestUrl(documentLocation);

        HttpResult httpResult;
        try {
            httpResult = httpRetriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new PageContentExtractorException("Error when contacting API for URL \"" + documentLocation + "\": "
                    + e.getMessage(), e);
        }

        extractedResult = httpResult.getStringContent();

        try {
            JsonObject json = new JsonObject(extractedResult);
            extractedResult = json.getString("text");
            extractedTitle = json.getString("title");
            extractedAuthor = json.getString("author");
            extractedDate = json.getString("date");
        } catch (JsonException e) {
            throw new PageContentExtractorException("Error while parsing the JSON response '"
                    + httpResult.getStringContent() + "': " + e.getMessage(), e);
        }

        return this;
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        return setDocument(document, true);
    }

    @Override
    public WebPageContentExtractor setDocument(Document document, boolean parse) throws PageContentExtractorException {
        String docUrl = document.getDocumentURI();
        return setDocument(docUrl, parse);
    }

    private String buildRequestUrl(String docUrl) {
        return String.format("http://www.diffbot.com/api/article?token=%s&url=%s", apiKey,
                UrlHelper.encodeParameter(docUrl));
    }

    @Override
    public Node getResultNode() {
        throw new UnsupportedOperationException("The DiffBotExtractor does not support main node extraction.");
    }

    @Override
    public String getResultText() {
        return extractedResult;
    }

    @Override
    public String getResultTitle() {
        return extractedTitle;
    }

    public String getResultAuthor() {
        return extractedAuthor;
    }

    public String getResultDate() {
        return extractedDate;
    }

    @Override
    public String getExtractorName() {
        return EXTRACTOR_NAME;
    }

    public static void main(String[] args) {
        DiffBotContentExtractor ce = new DiffBotContentExtractor("8a199dded2e23c8d13cab2742f1f4991");
        String resultText = ce.getResultText("http://www.bbc.co.uk/news/world-asia-17116595");
        // String resultText = ce
        // .getResultText("http://www.xconomy.com/san-francisco/2012/07/25/diffbot-is-using-computer-vision-to-reinvent-the-semantic-web/");

        System.out.println("title: " + ce.getResultTitle());
        System.out.println("text: " + resultText);
        System.out.println("author: " + ce.getResultAuthor());
        System.out.println("date: " + ce.getResultDate());
    }

}