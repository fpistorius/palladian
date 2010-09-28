/**
 *  The IFrameAnalyzer analyzes a webPage for existing IFrames and checks if their targets contains MIOs.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import tud.iir.helper.HTMLHelper;
import tud.iir.web.Crawler;

public class IFrameAnalyzer {

    /** The SearchWordMatcher. */
    private final transient SearchWordMatcher swMatcher;

    /**
     * Instantiates a new IFrameAnalyzer.
     * 
     * @param swMatcher the searchWordMatcher
     */
    public IFrameAnalyzer(final SearchWordMatcher swMatcher) {
        this.swMatcher = swMatcher;
    }

    /**
     * Check if the pages that are the target of the IFrame contain MIO-Indicators.
     * If yes than generate a new MIOPage-Object.
     * 
     * @param parentPageContent the parent page content
     * @param parentPageURL the parent page URL
     * @return the mioPages of IFrames
     */
    public List<MIOPage> getIframeMioPages(final String parentPageContent, final String parentPageURL) {
        final List<MIOPage> mioPages = new ArrayList<MIOPage>();
        final Crawler craw = new Crawler(5000,6000,9000);

        final List<String> iframeSources = analyzeForIframe(parentPageContent, parentPageURL);

        if (!iframeSources.isEmpty()) {
            final FastMIODetector mioDetector = new FastMIODetector();
            for (String iframeSourceURL : iframeSources) {

                // only analyze if the URL contains hints for the entity
                if (swMatcher.containsSearchWordOrMorphs(iframeSourceURL)) {
                    final Document webDocument = craw.getWebDocument(iframeSourceURL);
                    final String iframePageContent = Crawler.documentToString(webDocument);

                    // check for MIOs
                    if (!("").equals(iframePageContent)) {

                        if (mioDetector.containsMIO(iframePageContent)) {

                            final String parentPageTitle = extractParentPageTitle(parentPageContent);

                            final MIOPage mioPage = generateMIOPage(iframeSourceURL, parentPageURL, parentPageTitle,
                                    webDocument);
                            mioPages.add(mioPage);
                        }
                    }
                }
            }
        }
        return mioPages;
    }

    /**
     * Analyze a page for existing IFrames.
     * 
     * @param pageContent the page content
     * @param pageURL the page URL
     * @return the list
     */
    private List<String> analyzeForIframe(final String pageContent, final String pageURL) {
        List<String> iframeURLs;
        final List<String> iframeURLCandidates = new ArrayList<String>();
        if (pageContent.contains("<iframe") || pageContent.contains("<IFRAME")) {

            Pattern pattern = Pattern.compile("<iframe[^>]*src=[^>]*>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(pageContent);
            while (matcher.find()) {
                // first get the complete iframe-tag Result:
                // <iframe src="http://..."
                final String iframeTag = matcher.group(0);
                // extract the source; keep attention to ' vs " Result:
                // http://...
                iframeURLCandidates.addAll(getSrcFromIframe(iframeTag, "\""));
                iframeURLCandidates.addAll(getSrcFromIframe(iframeTag, "'"));

            }
        }
        iframeURLs = checkURLs(iframeURLCandidates, pageURL);
        return iframeURLs;
    }

    /**
     * extract the URL out of an IFrame-tag.
     * 
     * @param iframeTag the IFrame tag
     * @param quotMark the quotation mark
     * @return a List of URLs
     */
    private List<String> getSrcFromIframe(final String iframeTag, final String quotMark) {
        final List<String> iframeURLs = new ArrayList<String>();
        String pattern = "src=" + quotMark + "[^>" + quotMark + "]*" + quotMark;
        final String iframeSrc = HTMLHelper.extractTagElement(pattern, iframeTag, "src=");
        iframeURLs.add(iframeSrc);

        return iframeURLs;
    }

    /**
     * analyze the URLs for validness and eventually modify them e.g. relative Paths
     * 
     * @param urlCandidates the urlCandidates
     * @param parentPageURL the parent page URL
     * @return the list
     */
    private List<String> checkURLs(final List<String> urlCandidates, final String parentPageURL) {
       final List<String> validURLs = new ArrayList<String>();

        for (String urlCandidate : urlCandidates) {
            final String validURL = Crawler.verifyURL(urlCandidate, parentPageURL);
            if (!("").equals(validURL)) {
                validURLs.add(validURL);
            }
        }
        return validURLs;
    }

    /**
     * Create a new MIOPage.
     * 
     * @param iframeSourceURL the IFrame source URL
     * @param parentPageURL the parent page URL
     * @param parentPageTitle the parent page title
     * @param webDocument the web document
     * @return the mioPage
     */
    private MIOPage generateMIOPage(final String iframeSourceURL, final String parentPageURL, final String parentPageTitle,
            final Document webDocument) {
        final MIOPage mioPage = new MIOPage(iframeSourceURL, webDocument);
        mioPage.setIFrameSource(true);
        mioPage.setIframeParentPage(parentPageURL);
        mioPage.setIframeParentPageTitle(parentPageTitle);

        return mioPage;

    }

    /**
     * Extract parent page title.
     * 
     * @param pageContent the page content
     * @return the string
     */
    private String extractParentPageTitle(final String pageContent) {
        String title = "";
        String regExp = "<title>.*?</title>";
        final Pattern titlePattern = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher titleMatcher = titlePattern.matcher(pageContent);
        while (titleMatcher.find()) {
            title = titleMatcher.group(0);
            break;
        }
        return title.trim();
    }
}
