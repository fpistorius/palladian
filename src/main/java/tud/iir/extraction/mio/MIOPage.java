/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import org.w3c.dom.Document;

import tud.iir.web.Crawler;

/**
 * An webpage which contains mio(s).
 * 
 * @author Martin Werner
 */
public class MIOPage {

    /** The url. */
    private String url;

    /** The hostname. */
   final transient private String hostname;

   /** The title of a MIOPage. */
    private String title = "";

    /** To check if this MIOPage was linked. */
    private transient boolean isLinkedPage = false;

    /** The name of the link. */
    private String linkName = "";

    /** The title of the link. */
    private String linkTitle = "";

    /** The parent page that linked to this MIOPage. */
    private String linkParentPage = "";

    /** To check if this MIOPage is an iframe-source. */
    private transient boolean isIFrameSource = false;

    /** The iframe-Page that embeds this MIOPage. */
    private String iframeParentPage = "";

    /** The title of the iframe-page that embeds this MIOpage. */
    private String iframeParentPageTitle = "";

    /** The dedicated page trust. */
    private double dedicatedPageTrust = 0;

    // /** the document that is created after retrieving a web page */
    /** The web document. */
    final transient private Document webDocument;

    /**
     * Instantiates a new mIO page.
     * 
     * @param url the URL
     */
    public MIOPage(final String url) {
//        final Crawler crawler = new Crawler();
        final Crawler crawler = new Crawler(5000,6000,9000);
        
        this.url = url;
        this.webDocument = crawler.getWebDocument(url);
        this.hostname = Crawler.getDomain(url, false);
        this.title = Crawler.extractTitle(webDocument).trim();
    }

    /**
     * Instantiates a new mIO page.
     *
     * @param url the url
     * @param webDocument the web document
     */
    public MIOPage(final String url, final Document webDocument) {
        this.url = url;
        this.webDocument = webDocument;
        this.hostname = Crawler.getDomain(url, false);
        this.title = Crawler.extractTitle(webDocument).trim();
    }

    /**
     * Gets the url.
     * 
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     * 
     * @param url the new url
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * Gets the hostname.
     * 
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Checks if is i frame source.
     * 
     * @return true, if is i frame source
     */
    public boolean isIFrameSource() {
        return isIFrameSource;
    }

    /**
     * Sets the i frame source.
     * 
     * @param isIFrameSource the new i frame source
     */
    public void setIFrameSource(final boolean isIFrameSource) {
        this.isIFrameSource = isIFrameSource;
    }

    /**
     * Gets the content.
     * 
     * @return the content
     */
    public String getContentAsString() {
        return Crawler.documentToString(webDocument);

    }


    /**
     * Gets the link name.
     * 
     * @return the link name
     */
    public String getLinkName() {
        return linkName;
    }

    /**
     * Sets the link name.
     * 
     * @param linkName the new link name
     */
    public void setLinkName(final String linkName) {
        this.linkName = linkName;
    }

    /**
     * Gets the link parent page.
     * 
     * @return the link parent page
     */
    public String getLinkParentPage() {
        return linkParentPage;
    }

    /**
     * Sets the link parent page.
     * 
     * @param linkParentPage the new link parent page
     */
    public void setLinkParentPage(final String linkParentPage) {
        this.linkParentPage = linkParentPage;
    }

    /**
     * Checks if is linked page.
     * 
     * @return true, if is linked page
     */
    public boolean isLinkedPage() {
        return isLinkedPage;
    }

    /**
     * Sets the linked page.
     * 
     * @param isLinkedPage the new linked page
     */
    public void setLinkedPage(final boolean isLinkedPage) {
        this.isLinkedPage = isLinkedPage;
    }

    /**
     * Gets the link title.
     * 
     * @return the link title
     */
    public String getLinkTitle() {
        return linkTitle;
    }

    /**
     * Sets the link title.
     * 
     * @param linkTitle the new link title
     */
    public void setLinkTitle(final String linkTitle) {
        this.linkTitle = linkTitle;
    }

    /**
     * Gets the dedicated page trust.
     * 
     * @return the dedicated page trust
     */
    public double getDedicatedPageTrust() {
        return dedicatedPageTrust;
    }

    /**
     * Sets the dedicated page trust.
     * 
     * @param dedicatedPageTrust the new dedicated page trust
     */
    public void setDedicatedPageTrust(final double dedicatedPageTrust) {
        this.dedicatedPageTrust = dedicatedPageTrust;
    }

    /**
     * Gets the iframe parent page.
     * 
     * @return the iframe parent page
     */
    public String getIframeParentPage() {
        return iframeParentPage;
    }

    /**
     * Sets the iframe parent page.
     * 
     * @param iframeParentPage the new iframe parent page
     */
    public void setIframeParentPage(final String iframeParentPage) {
        this.iframeParentPage = iframeParentPage;
    }

    /**
     * Sets the iframe parent page title.
     * 
     * @param iframeParentPageTitle the new iframe parent page title
     */
    public void setIframeParentPageTitle(final String iframeParentPageTitle) {
        this.iframeParentPageTitle = iframeParentPageTitle;
    }

    /**
     * Gets the iframe parent page title.
     * 
     * @return the iframe parent page title
     */
    public String getIframeParentPageTitle() {
        return iframeParentPageTitle;
    }

    /**
     * Gets the title.
     * 
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     * 
     * @param title the new title
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    // public void setDocument(final Document document) {
    // this.webDocument = document;
    // }
    //
    /**
     * Gets the web document.
     *
     * @return the web document
     */
    public Document getWebDocument() {
        return webDocument;
    }

//    /**
//     * The main method.
//     *
//     * @param abc the arguments
//     */
//    public static void main(final String[] abc) {
//
//        MIOPage mioPage = new MIOPage("http://www.jr.com/canon/pe/CAN_MP990/");
//        // MIOPage mioPage2 = new MIOPage("http://www.gsmarena.com/samsung_s8500_wave-3d-spin-3146.php");
//    }

}
