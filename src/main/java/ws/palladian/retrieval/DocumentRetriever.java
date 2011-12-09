package ws.palladian.retrieval;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.ContentEncodingHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.SizeUnit;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

// TODO methods for parsing do not belong here and should be removed in the medium term
// TODO remove deprecated methods, after dependent code has been adapted
// TODO role of DownloadFilter is unclear, shouldn't the client itself take care about what to download?
// TODO completely remove all java.net.* stuff
// TODO remove properties configuration via file, dependend clients should set their preferences programmatically

/**
 * <p>
 * The DocumentRetriever allows to download pages from the Web or the hard disk.
 * </p>
 * <p>
 * You may configure it using the appropriate setter and getter methods or accept the default values.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class DocumentRetriever {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DocumentRetriever.class);

    // ///////////// constants with default configuration ////////

    /** The user agent string that is used by the crawler. */
    public static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.0.4) Gecko/2008102920 Firefox/3.0.4";

    /** The default timeout for a connection to be established, in milliseconds. */
    public static final long DEFAULT_CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    /** The default timeout which specifies the maximum interval for new packets to wait, in milliseconds. */
    public static final long DEFAULT_SOCKET_TIMEOUT = TimeUnit.SECONDS.toMillis(180);

    /** The default number of retries when downloading fails. */
    public static final int DEFAULT_NUM_RETRIES = 3;

    /** The number of threads for downloading in parallel. */
    public static final int DEFAULT_NUM_THREADS = 10;

    /** The default number of connections in the connection pool. */
    public static final int DEFAULT_NUM_CONNECTIONS = 100;

    // ///////////// Apache HttpComponents ////////

    /** Connection manager from Apache HttpComponents; thread safe and responsible for connection pooling. */
    private static ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager();

    /** Implementation of the Apache HttpClient. */
    private final ContentEncodingHttpClient httpClient;

    /** Various parameters for the Apache HttpClient. */
    private final HttpParams httpParams = new SyncBasicHttpParams();

    /** Identifier for Connection Metrics; see comment in constructor. */
    private final String CONTEXT_METRICS_ID = "CONTEXT_METRICS_ID";

    // ///////////// Settings ////////

    /** Download size in bytes for this DocumentRetriever instance. */
    private long totalDownloadedBytes = 0;

    /** Last download size in bytes for this DocumentRetriever. */
    private long lastDownloadedBytes = 0;

    /** Total number of bytes downloaded by all DocumentRetriever instances. */
    private static long sessionDownloadedBytes = 0;

    /** Total number of downloaded pages. */
    private static int numberOfDownloadedPages = 0;

    /** The filter for the retriever. */
    private DownloadFilter downloadFilter = new DownloadFilter();

    /** The maximum number of threads to use. */
    private int numThreads = DEFAULT_NUM_THREADS;

    // ///////////// Misc. ////////

    /** The callbacks that are called after each parsed page. */
    private final List<RetrieverCallback> retrieverCallbacks = new ArrayList<RetrieverCallback>();

    /** Hook for http* methods. */
    private HttpHook httpHook = new HttpHook.DefaultHttpHook();

    /** Factory for Document parsers. */
    private final ParserFactory parserFactory = new ParserFactory();

    /** Separator between HTTP header and content payload when writing HTTP results to file. */
    private static final String HTTP_RESULT_SEPARATOR = "\n----------------- End Headers -----------------\n\n";

    // ////////////////////////////////////////////////////////////////
    // constructor
    // ////////////////////////////////////////////////////////////////
    /**
     * Creates a new document retriever using default values for the parameters:
     * <table>
     * <tr>
     * <td>connection timeout</td>
     * <td>10 milliseconds</td>
     * </tr>
     * <tr>
     * <td>socket timeout</td>
     * <td>180 milliseconds</td>
     * </tr>
     * <tr>
     * <td>retries</td>
     * <td>3</td>
     * </tr>
     * <tr>
     * <td>maximum number of simultaneous threads</td>
     * <td>10</td>
     * </tr>
     * <tr>
     * <td>maximum number of simultanous connections</td>
     * <td>100</td>
     * </tr>
     * </table>
     * </p>
     **/
    public DocumentRetriever() {

        // initialize the HttpClient
        httpParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
        HttpProtocolParams.setUserAgent(httpParams, USER_AGENT);
        httpClient = new ContentEncodingHttpClient(connectionManager, httpParams);

        // setup the configuration; if no config available, use default values
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        setConnectionTimeout(config.getLong("documentRetriever.connectionTimeout", DEFAULT_CONNECTION_TIMEOUT));
        setSocketTimeout(config.getLong("documentRetriever.socketTimeout", DEFAULT_SOCKET_TIMEOUT));
        setNumRetries(config.getInt("documentRetriever.numRetries", DEFAULT_NUM_RETRIES));
        setNumConnections(config.getInt("documentRetriever.numConnections", DEFAULT_NUM_CONNECTIONS));

        /*
         * fix #261 to get connection metrics for head requests, see also discussion at
         * http://old.nabble.com/ConnectionShutdownException-when-trying-to-get-metrics-after-HEAD-request-td31358878.html
         * start code taken from apache, licensed as http://www.apache.org/licenses/LICENSE-2.0
         * http://svn.apache.org/viewvc/jakarta/jmeter/trunk/src/protocol/http/org/apache/jmeter/protocol/http/sampler/
         * HTTPHC4Impl.java?annotate=1090914&pathrev=1090914
         */
        HttpResponseInterceptor metricsSaver = new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
                HttpConnection conn = (HttpConnection)context.getAttribute(ExecutionContext.HTTP_CONNECTION);
                HttpConnectionMetrics metrics = conn.getMetrics();
                context.setAttribute(CONTEXT_METRICS_ID, metrics);
            }
        };

        ((AbstractHttpClient)httpClient).addResponseInterceptor(metricsSaver);
        // end edit
    }

    // ////////////////////////////////////////////////////////////////
    // HTTP methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Performs an HTTP GET operation.
     * 
     * @param url the URL for the GET.
     * @return response for the GET.
     * @throws HttpException in case the GET fails, or the supplied URL is not valid.
     */
    public HttpResult httpGet(String url) throws HttpException {
        return httpGet(url, Collections.<String, String>emptyMap());
    }

    /**
     * Performs an HTTP GET operation.
     * 
     * @param url the URL for the GET.
     * @param headers map with key-value pairs of request headers.
     * @return response for the GET.
     * @throws HttpException in case the GET fails, or the supplied URL is not valid.
     */
    public HttpResult httpGet(String url, Map<String, String> headers) throws HttpException {

        HttpGet get;
        try {
            get = new HttpGet(url);
        } catch (IllegalArgumentException e) {
            throw new HttpException("invalid URL: " + url, e);
        }

        for (Entry<String, String> header : headers.entrySet()) {
            get.setHeader(header.getKey(), header.getValue());
        }
        HttpResult result = execute(url, get);
        return result;
    }

    /**
     * Performs an HTTP HEAD operation.
     * 
     * @param url the URL for the HEAD.
     * @return response for the HEAD.
     * @throws HttpException in case the HEAD fails, or the supplied URL is not valid.
     */
    public HttpResult httpHead(String url) throws HttpException {
        HttpHead head;
        try {
            head = new HttpHead(url);
        } catch (Exception e) {
            throw new HttpException("invalid URL: " + url, e);
        }
        HttpResult result = execute(url, head);
        return result;
    }

    /**
     * Performs an HTTP POST operation with the specified name-value pairs as content.
     * 
     * @param url the URL for the POST.
     * @param content name-value pairs for the POST.
     * @return response for the POST.
     * @throws HttpException in case the POST fails, or the supplied URL is not valid.
     */
    public HttpResult httpPost(String url, Map<String, String> content) throws HttpException {
        return httpPost(url, Collections.<String, String>emptyMap(), content);
    }

    /**
     * Performs an HTTP POST operation with the specified name-value pairs as content.
     * 
     * @param url the URL for the POST.
     * @param headers map with key-value pairs of request headers.
     * @param content name-value pairs for the POST.
     * @return response for the POST.
     * @throws HttpException in case the POST fails, or the supplied URL is not valid.
     */
    public HttpResult httpPost(String url, Map<String, String> headers, Map<String, String> content)
            throws HttpException {
        HttpPost post;
        try {
            post = new HttpPost(url);
        } catch (Exception e) {
            throw new HttpException("invalid URL: " + url, e);
        }

        // HTTP headers
        for (Entry<String, String> header : headers.entrySet()) {
            post.setHeader(header.getKey(), header.getValue());
        }

        // content name-value pairs
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        for (Entry<String, String> param : content.entrySet()) {
            nameValuePairs.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        try {
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        }

        HttpResult result = execute(url, post);
        return result;
    }

    /**
     * Converts the Header type from Apache to a more generic Map.
     * 
     * @param headers
     * @return
     */
    private static Map<String, List<String>> convertHeaders(Header[] headers) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        for (Header header : headers) {
            List<String> list = result.get(header.getName());
            if (list == null) {
                list = new ArrayList<String>();
                result.put(header.getName(), list);
            }
            list.add(header.getValue());
        }
        return result;
    }

    /**
     * Internal method for executing the specified request; content of the result is read and buffered completely, up to
     * the specified limit in {@link DownloadFilter#getMaxFileSize()}.
     * 
     * @param url
     * @param request
     * @return
     * @throws HttpException
     */
    private HttpResult execute(String url, HttpUriRequest request) throws HttpException {
        HttpResult result;
        InputStream in = null;

        httpHook.beforeRequest(url, this);

        try {

            HttpContext context = new BasicHttpContext();
            HttpResponse response = httpClient.execute(request, context);
            HttpConnectionMetrics metrics = (HttpConnectionMetrics)context.getAttribute(CONTEXT_METRICS_ID);

            HttpEntity entity = response.getEntity();
            byte[] entityContent;

            if (entity != null) {

                in = entity.getContent();

                // check for a maximum download size limitation
                long maxFileSize = downloadFilter.getMaxFileSize();
                if (maxFileSize != -1) {
                    in = new BoundedInputStream(in, maxFileSize);
                }

                entityContent = IOUtils.toByteArray(in);

            } else {
                entityContent = new byte[0];
            }

            int statusCode = response.getStatusLine().getStatusCode();
            long receivedBytes = metrics.getReceivedBytesCount();
            Map<String, List<String>> headers = convertHeaders(response.getAllHeaders());
            result = new HttpResult(url, entityContent, headers, statusCode, receivedBytes);

            httpHook.afterRequest(result, this);
            addDownload(receivedBytes);

        } catch (IllegalStateException e) {
            throw new HttpException(e);
        } catch (IOException e) {
            throw new HttpException(e);
        } finally {
            IOUtils.closeQuietly(in);
            request.abort();
        }
        return result;
    }

    /**
     * Get the HTTP headers for a URL by sending a HEAD request.
     * 
     * @param url the URL of the page to get the headers from.
     * @return map with the headers, or an empty map if an error occurred.
     * @deprecated use {@link #httpHead(String)} and {@link HttpResult#getHeaders()} instead.
     */
    @Deprecated
    public Map<String, List<String>> getHeaders(String url) {
        Map<String, List<String>> result;
        try {
            HttpResult httpResult = httpHead(url);
            result = httpResult.getHeaders();
        } catch (HttpException e) {
            LOGGER.debug(e);
            result = Collections.emptyMap();
        }
        return result;
    }

    /**
     * Get the HTTP response code of the given URL after sending a HEAD request.
     * 
     * @param url the URL of the page to check for response code.
     * @return the HTTP response code, or -1 if an error occurred.
     * @deprecated use {@link #httpHead(String)} and {@link HttpResult#getStatusCode()} instead.
     */
    @Deprecated
    public int getResponseCode(String url) {
        int result;
        try {
            HttpResult httpResult = httpHead(url);
            result = httpResult.getStatusCode();
        } catch (HttpException e) {
            LOGGER.debug(e);
            result = -1;
        }
        return result;
    }

    /**
     * Gets the redirect URL from the HTTP "Location" header, if such exists.
     * 
     * @param url the URL to check for redirect.
     * @return redirected URL as String, or <code>null</code>.
     */
    public String getRedirectUrl(String url) {
        // TODO should be changed to use HttpComponents
        String location = null;
        try {
            URL urlObject = new URL(url);
            URLConnection urlCon = urlObject.openConnection();
            HttpURLConnection httpUrlCon = (HttpURLConnection)urlCon;
            httpUrlCon.setInstanceFollowRedirects(false);
            location = httpUrlCon.getHeaderField("Location");
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return location;
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing (X)HTML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get a web page ((X)HTML document).
     * 
     * @param url The URL or file path of the web page.
     * @return The W3C document.
     */
    public Document getWebDocument(String url) {
        return getDocument(url, false);
    }

    /**
     * Get multiple URLs in parallel, for each finished download the supplied callback is invoked.
     * 
     * @param urls the URLs to download.
     * @param callback the callback to be called for each finished download.
     */
    public void getWebDocuments(Collection<String> urls, RetrieverCallback callback) {

        BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>(urls);

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Runnable runnable = new DocumentRetrieverThread(urlQueue, callback, this);
            threads[i] = new Thread(runnable);
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     * Get multiple URLs in parallel.
     * 
     * @param urls the URLs to download.
     * @return set with the downloaded documents.
     */
    public Set<Document> getWebDocuments(Collection<String> urls) {
        final Set<Document> result = new HashSet<Document>();
        getWebDocuments(urls, new RetrieverCallback() {
            @Override
            public void onFinishRetrieval(Document document) {
                synchronized (result) {
                    result.add(document);
                }
            }
        });
        return result;
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing XML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get XML document from a URL. Pure XML documents can created with the native DocumentBuilderFactory, which works
     * better with the native XPath queries.
     * 
     * @param url The URL or file path pointing to the XML document.
     * @return The XML document.
     */
    public Document getXMLDocument(String url) {
        return getDocument(url, true);
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing JSON documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get a JSON object from a URL. The retrieved contents must return a valid JSON object.
     * TODO rename this to getJSONObject
     * 
     * @param url the URL pointing to the JSON string.
     * @return the JSON object.
     */
    public JSONObject getJSONDocument(String url) {
        String json = getTextDocument(url);

        // delicous feeds return the whole JSON object wrapped in [square brackets],
        // altough this seems to be valid, our parser doesn't like this, so we remove
        // those brackets before parsing -- Philipp, 2010-07-04
        if (json != null) {
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
            }

            JSONObject jsonOBJ = null;

            if (json.length() > 0) {
                try {
                    jsonOBJ = new JSONObject(json);
                } catch (JSONException e) {
                    LOGGER.error(url + ", " + e.getMessage(), e);
                }
            }

            return jsonOBJ;
        }
        return null;
    }

    /**
     * Get a JSON array from a URL. The retrieved contents must return a valid JSON array.
     * 
     * @param url the URL pointing to the JSON string.
     * @return the JSON array.
     */
    public JSONArray getJSONArray(String url) {
        String json = getTextDocument(url);

        // since we know this string should be an JSON array,
        // we will directly parse it
        if (json != null) {
            json = json.trim();

            JSONArray jsonAR = null;

            if (json.length() > 0) {
                try {
                    jsonAR = new JSONArray(json);
                } catch (JSONException e) {
                    LOGGER.error(url + ", " + e.getMessage(), e);
                }
            }

            return jsonAR;

        }
        return null;

    }

    // ////////////////////////////////////////////////////////////////
    // method for retrieving plain text
    // ////////////////////////////////////////////////////////////////

    /**
     * Download the contents that are retrieved from the given URL.
     * 
     * @param url The URL of the desired contents.
     * @return The contents as a string or <code>null</code> if contents could no be retrieved. See the error log for
     *         possible errors.
     */
    public String getTextDocument(String url) {

        String contentString = null;
        Reader reader = null;

        if (downloadFilter.isAcceptedFileType(url)) {
            try {
                if (isFile(url)) {
                    reader = new FileReader(url);
                    contentString = IOUtils.toString(reader);
                } else {
                    HttpResult httpResult = httpGet(url);
                    contentString = new String(httpResult.getContent());
                }
            } catch (IOException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        return contentString;
    }

    // ////////////////////////////////////////////////////////////////
    // internal methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Multi-purpose method to get a {@link Document}, either by downloading it from the Web, or by reading it from
     * disk. The document may be parsed using an XML parser or a dedicated (X)HTML parser.
     * 
     * @param url the URL of the document to retriever or the file path.
     * @param xml indicate whether the document is well-formed XML or needs to be processed using an (X)HTML parser.
     * @return the parsed document, or <code>null</code> if any kind of error occurred or the document was filtered by
     *         {@link DownloadFilter}.
     */
    private Document getDocument(String url, boolean xml) {

        Document document = null;
        String cleanUrl = url.trim();
        InputStream inputStream = null;

        if (downloadFilter.isAcceptedFileType(cleanUrl)) {

            try {

                if (isFile(cleanUrl)) {
                    File file = new File(cleanUrl);
                    inputStream = new BufferedInputStream(new FileInputStream(new File(cleanUrl)));
                    document = parse(inputStream, xml);
                    document.setDocumentURI(file.toURI().toString());
                } else {
                    HttpResult httpResult = httpGet(cleanUrl);
                    document = parse(new ByteArrayInputStream(httpResult.getContent()), xml);
                    document.setDocumentURI(cleanUrl);
                }

                callRetrieverCallback(document);

            } catch (FileNotFoundException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (DOMException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (ParserException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (HttpException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        return document;
    }

    private static boolean isFile(String url) {
        boolean isFile = false;
        if (url.indexOf("http://") == -1 && url.indexOf("https://") == -1) {
            isFile = true;
        }
        return isFile;
    }

    /**
     * Parses a an {@link InputStream} to a {@link Document}.
     * 
     * @param inputStream the stream to parse.
     * @param xml <code>true</code> if this document is an XML document, <code>false</code> if HTML document.
     * @throws ParserException if parsing failed.
     */
    private Document parse(InputStream inputStream, boolean xml) throws ParserException {
        Document document = null;
        DocumentParser parser;

        if (xml) {
            parser = parserFactory.createXmlParser();
        } else {
            parser = parserFactory.createHtmlParser();
        }

        document = parser.parse(inputStream);
        return document;
    }

    // ////////////////////////////////////////////////////////////////
    // methods for downloading files
    // ////////////////////////////////////////////////////////////////

    /**
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath) {
        return downloadAndSave(url, filePath, new HashMap<String, String>(), false);
    }

    /**
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to.
     * @param includeHttpResponseHeaders whether to prepend the received HTTP headers for the request to the saved
     *            content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath, boolean includeHttpResponseHeaders) {
        return downloadAndSave(url, filePath, new HashMap<String, String>(), includeHttpResponseHeaders);
    }

    /**
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to; if file name ends with ".gz", the file
     *            is compressed automatically.
     * @param requestHeaders The headers to include in the request.
     * @param includeHttpResponseHeaders whether to prepend the received HTTP headers for the request to the saved
     *            content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath, Map<String, String> requestHeaders,
            boolean includeHttpResponseHeaders) {

        boolean result = false;
        try {
            HttpResult httpResult = httpGet(url, requestHeaders);
            result = saveToFile(httpResult, filePath, includeHttpResponseHeaders);
        } catch (HttpException e) {
            LOGGER.error(e);
        }

        return result;
    }

    /**
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * 
     * @param httpResult The httpResult to save.
     * @param filePath the path where the downloaded contents should be saved to; if file name ends with ".gz", the file
     *            is compressed automatically.
     * @param includeHttpResponseHeaders whether to prepend the received HTTP headers for the request to the saved
     *            content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean saveToFile(HttpResult httpResult, String filePath, boolean includeHttpResponseHeaders) {

        boolean result = false;
        boolean compress = filePath.endsWith(".gz") || filePath.endsWith(".gzip");
        OutputStream out = null;

        try {
            out = new BufferedOutputStream(new FileOutputStream(filePath));

            if (compress) {
                out = new GZIPOutputStream(out);
            }

            if (includeHttpResponseHeaders) {

                StringBuilder headerBuilder = new StringBuilder();
                headerBuilder.append("Status Code").append(":");
                headerBuilder.append(httpResult.getStatusCode()).append("\n");

                Map<String, List<String>> headers = httpResult.getHeaders();

                for (Entry<String, List<String>> headerField : headers.entrySet()) {
                    headerBuilder.append(headerField.getKey()).append(":");
                    headerBuilder.append(StringUtils.join(headerField.getValue(), ","));
                    headerBuilder.append("\n");
                }

                headerBuilder.append(HTTP_RESULT_SEPARATOR);

                // TODO should be set to UTF-8 explicitly,
                // but I do not want to change this now.
                IOUtils.write(headerBuilder, out);

            }

            IOUtils.write(httpResult.getContent(), out);
            result = true;

        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            FileHelper.close(out);
        }

        return result;

    }

    /**
     * Load a GZIP dataset file and return a {@link HttpResult}.
     * 
     * @param file
     * @return The http result from file or <code>null</code> on in case an {@link IOException} was caught.
     */
    // TODO should this be extended to handle files without the written header?
    public HttpResult loadSerializedGzip(File file) {

        HttpResult httpResult = null;
        InputStream inputStream = null;

        try {
            // we don't know this anymore
            String url = "from_file_system";
            Map<String, List<String>> headers = new HashMap<String, List<String>>();

            // we don't know this anymore
            long transferedBytes = -1;

            // Wrap this with a GZIPInputStream, if necessary.
            // Do not use InputStreamReader, as this works encoding specific.
            inputStream = new GZIPInputStream(new FileInputStream(file));

            // Read the header information, until the HTTP_RESULT_SEPARATOR is reached.
            // We assume here, that one byte resembles one character, which is not true
            // in general, but should suffice in our case. Hopefully.
            StringBuilder headerText = new StringBuilder();
            int b;
            while ((b = inputStream.read()) != -1) {
                headerText.append((char)b);
                if (headerText.toString().endsWith(HTTP_RESULT_SEPARATOR)) {
                    break;
                }
            }
            int statusCode = parseHeaders(headerText.toString(), headers);

            // Read the payload.
            ByteArrayOutputStream payload = new ByteArrayOutputStream();
            while ((b = inputStream.read()) != -1) {
                payload.write(b);
            }
            byte[] content = payload.toByteArray();
            httpResult = new HttpResult(url, content, headers, statusCode, transferedBytes);

        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            FileHelper.close(inputStream);
        }

        return httpResult;
    }

    /**
     * Extract header information from the supplied string. The header data is put in the Map, the HTTP status code is
     * returned.
     * 
     * @param headerText newline separated HTTP header text.
     * @param headers out-parameter for parsed HTTP headers.
     * @return the HTTP status code.
     */
    private int parseHeaders(String headerText, Map<String, List<String>> headers) {
        String[] headerLines = headerText.split("\n");
        int statusCode = -1;
        for (String headerLine : headerLines) {
            String[] parts = headerLine.split(":");
            if (parts.length > 1) {
                if (parts[0].equalsIgnoreCase("status code")) {
                    try {
                        String statusCodeString = parts[1];
                        statusCodeString = statusCodeString.replace("HTTP/1.1", "");
                        statusCodeString = statusCodeString.replace("OK", "");
                        statusCodeString = statusCodeString.trim();
                        statusCode = Integer.valueOf(statusCodeString);
                    } catch (Exception e) {
                        LOGGER.error(e);
                    }
                } else {

                    StringBuilder valueString = new StringBuilder();
                    for (int i = 1; i < parts.length; i++) {
                        valueString.append(parts[i]).append(":");
                    }
                    String valueStringClean = valueString.toString();
                    if (valueStringClean.endsWith(":")) {
                        valueStringClean = valueStringClean.substring(0, valueStringClean.length() - 1);
                    }

                    ArrayList<String> values = new ArrayList<String>();

                    // in cases we have a "=" we can split on comma
                    if (valueStringClean.contains("=")) {
                        String[] valueParts = valueStringClean.split(",");
                        for (String valuePart : valueParts) {
                            values.add(valuePart.trim());
                        }
                    } else {
                        values.add(valueStringClean);
                    }

                    headers.put(parts[0], values);
                }
            }
        }
        return statusCode;
    }

    /**
     * Download a binary file from specified URL to a given path.
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to.
     * @return the file were the downloaded contents were saved to.
     * @author Martin Werner
     * @deprecated use {@link #downloadAndSave(String, String)} instead.
     */
    @Deprecated
    public static File downloadBinaryFile(String url, String filePath) {
        File file = null;
        DocumentRetriever documentRetriever = new DocumentRetriever();
        boolean success = documentRetriever.downloadAndSave(url, filePath);
        if (success) {
            file = new File(filePath);
        }
        return file;
    }

    // ////////////////////////////////////////////////////////////////
    // Traffic count and statistics
    // ////////////////////////////////////////////////////////////////

    /**
     * To be called after downloading data from the web.
     * 
     * @param size the size in bytes that should be added to the download counters.
     */
    private synchronized void addDownload(long size) {
        totalDownloadedBytes += size;
        lastDownloadedBytes = size;
        sessionDownloadedBytes += size;
        numberOfDownloadedPages++;
    }

    public long getLastDownloadSize() {
        return lastDownloadedBytes;
    }

    public long getLastDownloadSize(SizeUnit unit) {
        return unit.convert(lastDownloadedBytes, SizeUnit.BYTES);
    }

    public long getTotalDownloadSize() {
        return getTotalDownloadSize(SizeUnit.BYTES);
    }

    public long getTotalDownloadSize(SizeUnit unit) {
        return unit.convert(totalDownloadedBytes, SizeUnit.BYTES);
    }

    public static long getSessionDownloadSize() {
        return getSessionDownloadSize(SizeUnit.BYTES);
    }

    public static long getSessionDownloadSize(SizeUnit unit) {
        return unit.convert(sessionDownloadedBytes, SizeUnit.BYTES);
    }

    public static int getNumberOfDownloadedPages() {
        return numberOfDownloadedPages;
    }

    public void resetDownloadSizes() {
        totalDownloadedBytes = 0;
        lastDownloadedBytes = 0;
        sessionDownloadedBytes = 0;
        numberOfDownloadedPages = 0;
    }

    public static void resetSessionDownloadSizes() {
        sessionDownloadedBytes = 0;
        numberOfDownloadedPages = 0;
    }

    // ////////////////////////////////////////////////////////////////
    // Configuration options
    // ////////////////////////////////////////////////////////////////

    public void setConnectionTimeout(long connectionTimeout) {
        HttpConnectionParams.setConnectionTimeout(httpParams, (int)connectionTimeout);
    }

    public long getConnectionTimeout() {
        return HttpConnectionParams.getConnectionTimeout(httpParams);
    }

    /**
     * <p>
     * Resets this {@code DocumentRetriever}s socket timeout time overwriting the old value. The default value for this
     * attribute after initialisation is 180 milliseconds.
     * </p>
     * 
     * @param socket timeout The new socket timeout time in milliseconds
     */
    public void setSocketTimeout(long socketTimeout) {
        HttpConnectionParams.setSoTimeout(httpParams, (int)socketTimeout);
    }

    /**
     * <p>
     * Provides this {@code DocumentRetriever}s socket timeout time. The default value set upon initialisation is 180
     * milliseconds.
     * </p>
     * 
     * @return The socket timeout time of this {@code DocumentRetriever} in milliseconds.
     */
    public long getSocketTimeout() {
        return HttpConnectionParams.getSoTimeout(httpParams);
    }

    /**
     * Set the maximum number of simultaneous threads for downloading.
     * 
     * @param numThreads the number of threads to use.
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public void setNumRetries(int numRetries) {
        HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(numRetries, false);
        httpClient.setHttpRequestRetryHandler(retryHandler);
    }

    public void setNumConnections(int numConnections) {
        connectionManager.setMaxTotal(numConnections);
    }

    public void setDownloadFilter(DownloadFilter downloadFilter) {
        this.downloadFilter = downloadFilter;
    }

    public DownloadFilter getDownloadFilter() {
        return downloadFilter;
    }

    /**
     * Sets the current Proxy.
     * 
     * @param proxy the proxy to use.
     */
    public void setProxy(Proxy proxy) {
        InetSocketAddress address = (InetSocketAddress)proxy.address();
        String hostname = address.getHostName();
        int port = address.getPort();
        setProxy(hostname, port);
    }

    public void setProxy(String hostname, int port) {
        HttpHost proxy = new HttpHost(hostname, port);
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        LOGGER.debug("set proxy to " + hostname + ":" + port);
    }

    public void setProxy(String proxy) {
        String[] split = proxy.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("argument must be hostname:port");
        }
        String hostname = split[0];
        int port = Integer.valueOf(split[1]);
        setProxy(hostname, port);
    }

    // ////////////////////////////////////////////////////////////////
    // Callbacks
    // ////////////////////////////////////////////////////////////////

    private void callRetrieverCallback(Document document) {
        for (RetrieverCallback retrieverCallback : retrieverCallbacks) {
            retrieverCallback.onFinishRetrieval(document);
        }
    }

    public List<RetrieverCallback> getRetrieverCallbacks() {
        return retrieverCallbacks;
    }

    public void addRetrieverCallback(RetrieverCallback retrieverCallback) {
        retrieverCallbacks.add(retrieverCallback);
    }

    public void removeRetrieverCallback(RetrieverCallback retrieverCallback) {
        retrieverCallbacks.remove(retrieverCallback);
    }

    public void setHttpHook(HttpHook httpHook) {
        this.httpHook = httpHook;
    }

    // ////////////////////////////////////////////////////////////////
    // main method
    // ////////////////////////////////////////////////////////////////

    /**
     * The main method for testing and usage purposes.
     * 
     * @param args The arguments.
     */
    public static void main(String[] args) throws Exception {

        // #261 example code
        DocumentRetriever retriever = new DocumentRetriever();

        // String filePath = "/home/pk/1312910093553_2011-08-09_19-14-53.gz";
        // HttpResult httpResult = retriever.loadSerializedGzip(new File(filePath));
        // XmlParser parser = new XmlParser();
        // Document document = parser.parse(httpResult);
        // System.out.println(HTMLHelper.getXmlDump(document));
        // System.exit(0);
        //
        //
        // // Wrap this with a GZIPInputStream, if necessary.
        // // Do not use InputStreamReader, as this works encoding specific.
        // InputStream inputStream = new FileInputStream(new File(filePath));
        // inputStream = new GZIPInputStream(inputStream);
        //
        // // Read the header information, until the HTTP_RESULT_SEPARATOR is reached.
        // // We assume here, that one byte resembles one character, which is not true
        // // in general, but should suffice in our case. Hopefully.
        // StringBuilder headerText = new StringBuilder();
        // int b;
        // while ((b = inputStream.read()) != -1) {
        // headerText.append((char) b);
        // if (headerText.toString().endsWith(HTTP_RESULT_SEPARATOR)) {
        // break;
        // }
        // }
        //
        // // Read the payload.
        // ByteArrayOutputStream payload = new ByteArrayOutputStream();
        // while ((b = inputStream.read()) != -1) {
        // payload.write(b);
        // }
        //
        // // Try to parse.
        // //Document document = parser.parse(new ByteArrayInputStream(payload.toByteArray()));
        // System.out.println(headerText.toString());
        // System.out.println("===================");

        System.exit(0);

        // HttpResult result = retriever.httpGet(url);
        // String eTag = result.getHeaderString("Last-Modified");
        //
        // Map<String, String> header = new HashMap<String, String>();
        // header.put("If-Modified-Since", eTag);
        //
        // retriever.httpGet(url, header);
        // System.exit(0);
        //
        // // download and save a web page including their headers in a gzipped file
        // retriever.downloadAndSave("http://cinefreaks.com", "data/temp/cf_no_headers.gz", new HashMap<String,
        // String>(),
        // true);

        // create a retriever that is triggered for every retrieved page
        RetrieverCallback crawlerCallback = new RetrieverCallback() {
            @Override
            public void onFinishRetrieval(Document document) {
                // do something with the page
                LOGGER.info(document.getDocumentURI());
            }
        };
        retriever.addRetrieverCallback(crawlerCallback);

        // give the retriever a list of URLs to download
        Set<String> urls = new HashSet<String>();
        urls.add("http://www.cinefreaks.com");
        urls.add("http://www.imdb.com");

        // set the maximum number of threads to 10
        retriever.setNumThreads(10);

        // download documents
        Set<Document> documents = retriever.getWebDocuments(urls);
        CollectionHelper.print(documents);

        // or just get one document
        Document webPage = retriever.getWebDocument("http://www.cinefreaks.com");
        LOGGER.info(webPage.getDocumentURI());

    }
}