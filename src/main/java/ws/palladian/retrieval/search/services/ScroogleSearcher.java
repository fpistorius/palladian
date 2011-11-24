package ws.palladian.retrieval.search.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.NekoHtmlParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcher;

/**
 * <p>
 * Web searcher which scrapes content from Scroogle.
 * </p>
 * 
 * @author Eduardo Jacobo Miranda
 * @author Philipp Katz
 */
public final class ScroogleSearcher extends BaseWebSearcher implements WebSearcher {
    

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ScroogleSearcher.class);

    private final DocumentParser parser;

    public ScroogleSearcher() {
        super();
        parser = new NekoHtmlParser();
    }

    // TODO currently, paging/result count is not supported
    @Override
    public List<WebResult> search(String query) {

        List<WebResult> result = new ArrayList<WebResult>();

        try {

            String requestUrl = "http://www.scroogle.org/cgi-bin/nbbwssl.cgi?Gw=" + UrlHelper.urlEncode(query);
            HttpResult httpResult = retriever.httpGet(requestUrl);
            Document document = parser.parse(httpResult);

            List<Node> linkNodes = XPathHelper.getXhtmlNodes(document, "//font/blockquote/a");
            List<Node> infoNodes = XPathHelper.getXhtmlNodes(document, "//font/blockquote/ul/font");

            if (linkNodes.size() != infoNodes.size()) {
                throw new IllegalStateException(
                        "The returned document structure is not as expected, probably the scraper needs to be updated");
            }

            Iterator<Node> linkIterator = linkNodes.iterator();
            Iterator<Node> infoIterator = infoNodes.iterator();
            int rank = 1;

            while (linkIterator.hasNext()) {
                Node linkNode = linkIterator.next();
                Node infoNode = infoIterator.next();

                String url = linkNode.getAttributes().getNamedItem("href").getTextContent();
                String title = linkNode.getTextContent();

                // the summary needs some cleaning; what we want is between "quotes",
                // we also remove double whitespaces
                String summary = infoNode.getTextContent();
                summary = StringHelper.getSubstringBetween(summary, "\"", "\"");
                summary = StringHelper.removeDoubleWhitespaces(summary);

                WebResult webResult = new WebResult(0, rank++, url, title, summary);
                result.add(webResult);

            }
            
            // debugging
            if (result.isEmpty()) {
                System.out.println(HtmlHelper.documentToString(document));
                System.exit(0);
            }

        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (ParserException e) {
            LOGGER.error(e);
        }

        return result;

    }
    
    @Override
    public String getName() {
        return "Scroogle";
    }

    public static void main(String[] args) {
        WebSearcher webSearcher = new ScroogleSearcher();
        
        // let's see, how far we can go:
        List<String> queries = FileHelper.readFileToArray("/Users/pk/Uni/feeddataset/gathering_TUDCS6/finalQueries-TUDCS6.txt");
        int counter = 0;
        for (String query : queries) {
            
            
            List<WebResult> searchResult = webSearcher.search(query);
            counter++;
            
            System.out.println(counter + "\t" + searchResult.size());
            
        }
        
        
//        List<WebResult> results = webSearcher.search("the us");
//        for (WebResult webResult : results) {
//            System.out.println(webResult.getRank());
//            System.out.println(webResult.getTitle());
//            System.out.println(webResult.getSummary());
//            System.out.println(webResult.getUrl());
//            System.out.println("-------------------");
//        }
    }

}
