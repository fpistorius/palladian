package ws.palladian.helper.html;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * A helper class for handling XPath queries.
 * </p>
 *
 * <p>
 * The methods {@link #getNodes(Node, String, Map)} and {@link #getNode(Node, String, Map)} allow processing of
 * Documents with namespaces. If your XPath expression contains namespace prefixes, like <code>//atom:entry</code>, you
 * must supply the corresponding mapping from prefix to URI as parameter, for example:
 *
 * <pre>
 * Map&lt;String, String&gt; mapping = new HashMap&lt;String, String&gt;();
 * mapping.put(&quot;atom&quot;, &quot;http://www.w3.org/2005/Atom&quot;);
 * List&lt;Node&gt; nodes = XPathHelper.getNodes(doc, &quot;//atom:entry&quot;, mapping);
 * </pre>
 * <p>
 * The methods with <code>Xhtml</code> in their names, like {@link #getXhtmlNodes(Document, String)}, serve as
 * convenience methods for processing XHTML documents. The prefix for XHTML namespace is inserted automatically,
 * simplifying XHTML XPath queries.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class XPathHelper {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XPathHelper.class);

    /**
     * XHTML namespace URI.
     */
    private static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    private static final String XHTML_COLON = "xhtml:";

    private static final Pattern AND_OR = Pattern.compile("and|or");
    private static final Pattern XHTML_TAGGABLE = Pattern.compile("[a-zA-Z][\\w]*|\\*");
    private static final Pattern XHTML_NS = Pattern.compile("(\"[^\"]+\")|('[^']+')");

    private static class MyNamespaceContext implements NamespaceContext {
        private final Map<String, String> namespaces = new HashMap<>();

        @Override
        public String getNamespaceURI(String prefix) {
            return namespaces.get(prefix);
        }

        // This method isn't necessary for XPath processing.
        @Override
        public String getPrefix(String uri) {
            throw new UnsupportedOperationException();
        }

        // This method isn't necessary for XPath processing either.
        @Override
        public Iterator<?> getPrefixes(String uri) {
            throw new UnsupportedOperationException();
        }

        public void addNamespace(String prefix, String uri) {
            namespaces.put(prefix, uri);
        }

    }

    private XPathHelper() {
        // utility class, prevent instantiation.
    }

    /**
     * <p>
     * Get a list of {@link Node}s matching the given XPath expression.
     * </p>
     *
     * @param node       The Node or Document to consider, not <code>null</code>.
     * @param xPath      The XPath expression, not <code>null</code> or empty.
     * @param namespaces (Optional) Map with namespaces, necessary to bind prefixes in XPath expression to namespaces.
     * @return Matching nodes for the given XPath expression, or an empty {@link List} if no nodes match or an error
     * occurred.
     */
    public static List<Node> getNodes(Node node, String xPath, Map<String, String> namespaces) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");

        List<Node> ret = new ArrayList<>();

        XPathFactory factory = XPathFactory.newInstance();
        XPath xPathObject = factory.newXPath();

        MyNamespaceContext namespaceContext = new MyNamespaceContext();
        namespaceContext.addNamespace("xhtml", XHTML_NAMESPACE);
        if (namespaces != null) {
            for (Entry<String, String> entry : namespaces.entrySet()) {
                String prefix = entry.getKey();
                String uri = entry.getValue();
                namespaceContext.addNamespace(prefix, uri);
            }
        }
        xPathObject.setNamespaceContext(namespaceContext);
        try {
            XPathExpression xPathExpression = xPathObject.compile(xPath);
            NodeList nodes = (NodeList) xPathExpression.evaluate(node, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                ret.add(nodes.item(i));
            }
        } catch (XPathExpressionException e) {
            // TODO this exception should be thrown
            LOGGER.error("{} for XPath \"{}\" : {}", new Object[]{e, xPath, e.getMessage(), e});
        }

        return ret;
    }

    /**
     * <p>
     * Get a list of {@link Node}s from matching the given XPath expression.
     * </p>
     *
     * @param node  The Node or Document to consider, not <code>null</code>.
     * @param xPath The XPath expression, not <code>null</code> or empty.
     * @return Matching nodes for the given XPath expression, or an empty {@link List} if no nodes match or an error
     * occurred.
     */
    public static List<Node> getNodes(Node node, String xPath) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");
        return getNodes(node, xPath, null);
    }

    /**
     * <p>
     * Get a {@link Node} matching the given XPath expression.
     * </p>
     *
     * @param node       The Node or Document to consider, not <code>null</code>.
     * @param xPath      The XPath expression, not <code>null</code> or empty.
     * @param namespaces (Optional) {@link Map} with namespaces, necessary to bind prefixes in XPath expression to
     *                   namespaces.
     * @return Matching node for the given xPath expression, or <code>null</code> if no matching node or an error
     * occurred.
     */
    public static Node getNode(Node node, String xPath, Map<String, String> namespaces) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");
        List<Node> nodeList = getNodes(node, xPath, namespaces);
        return CollectionHelper.getFirst(nodeList);
    }

    /**
     * <p>
     * Get a {@link Node} matching the given XPath expression.
     * </p>
     *
     * @param node  The Node or Document to consider, not <code>null</code>.
     * @param xPath The XPath expression, not <code>null</code> or empty.
     * @return Matching node for the given XPath expression, or <code>null</code> if no matching node or an error
     * occurred.
     */
    public static Node getNode(Node node, String xPath) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");
        return getNode(node, xPath, null);
    }

    /**
     * <p>
     * Get the {@link Node} with the specified ID.
     * </p>
     *
     * @param node   The Node or Document to consider, not <code>null</code>.
     * @param nodeId The ID of the Node to return, not <code>null</code> or empty.
     * @return Matching node with the given ID, or <code>null</code> if no matching node.
     */
    public static Node getNodeByID(Node node, String nodeId) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(nodeId, "nodeId must not be empty.");

        List<Node> idNodes = XPathHelper.getNodes(node, "//*[@id='" + nodeId + "']");
        return CollectionHelper.getFirst(idNodes);
    }

    /**
     * <p>
     * Get the parent of the {@link Node} with the specified ID.
     * </p>
     *
     * @param node   The Node or Document to consider, not <code>null</code>.
     * @param nodeId The ID of the Node to return, not <code>null</code> or empty.
     * @return Parent of the node with the given ID, or <code>null</code> if no matching node.
     */
    public static Node getParentNodeByID(Node node, String nodeId) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(nodeId, "nodeId must not be empty.");
        Node result = null;
        Node childNode = getNodeByID(node, nodeId);
        if (childNode != null) {
            result = childNode.getParentNode();
        }
        return result;
    }

    /**
     * <p>
     * Get the text content of a child node with the given XPath expression.
     * </p>
     *
     * @param node  The node whose children are considered, not <code>null</code>.
     * @param xPath The XPath expression addressing the node with the sought text content, not <code>null</code> or
     *              empty.
     * @return The Node's text content, or an empty {@link String} if node does not exist.
     */
    public static String getNodeTextContent(Node node, String xPath) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");

        String textContent = "";
        Node textNode = getNode(node, xPath);
        if (textNode != null) {
            textContent = textNode.getTextContent();
        }

        return textContent;
    }

    /**
     * <p>
     * Get the text content of a child node with the given XPath expression.
     * </p>
     *
     * @param node  The node whose children are considered, not <code>null</code>.
     * @param xPath The XPath expression addressing the node with the sought text content, not <code>null</code> or
     *              empty.
     * @return The Node's text content, or an empty {@link String} if node does not exist.
     */
    public static String getXhtmlNodeTextContent(Node node, String xPath) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");

        String textContent = "";
        Node textNode = getXhtmlNode(node, xPath);
        if (textNode != null) {
            textContent = textNode.getTextContent();
        }

        return textContent;
    }
    public static String tryGetXhtmlNodeTextContent(Node node, String xPath) {
        try {
            return getXhtmlNodeTextContent(node, xPath);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return "";
    }

    /**
     * <p>
     * Get the XPath that points to the parent element of the given XPath. For example: <code>/DIV/P/A</code> =>
     * <code>/DIV/P</code>.
     * </p>
     *
     * @param xPath The xPath for which the parent XPath should be found, not <code>null</code> or empty.
     * @return The XPath that points to the parent node of the given XPath.
     */
    public static String getParentXPath(String xPath) {
        Validate.notEmpty(xPath, "xPath must not be empty");

        String parentXPath = xPath;

        int i = xPath.lastIndexOf("/");
        if (i > -1) {
            parentXPath = xPath.substring(0, i);
        }

        return parentXPath;
    }

    /**
     * <p>
     * Gets the previous sibling {@link Node}s of a {@link Node}.
     * </p>
     *
     * @param node the node
     * @return the previous siblings
     */
    public static List<Node> getPreviousSiblings(Node node) {
        Validate.notNull(node, "node must not be null");
        Node parentNode = node.getParentNode();
        List<Node> previousSiblings = new ArrayList<>();
        NodeList childNodes = parentNode.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.isSameNode(node)) {
                break;
            } else {
                previousSiblings.add(childNode);
            }
        }
        return previousSiblings;
    }

    // //////////////////////////////////////////////////
    // convenience methods for XHTML XPaths
    // //////////////////////////////////////////////////

    /**
     * <p>
     * Get a list of {@link Node}s from the supplied XHTML {@link Document} matching the given XPath expression. The
     * XHTML namespace prefixes are inserted automatically.
     * </p>
     *
     * @param document The {@link Document} to consider, not <code>null</code>.
     * @param xPath    The XPath expression, not <code>null</code> or empty.
     * @return Matching nodes for the given XPath expression, or an empty {@link List} if no nodes match or an error
     * occurred.
     */
    public static List<Node> getXhtmlNodes(Document document, String xPath) {
        Validate.notNull(document, "document must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");
        return getNodes(document.getLastChild(), addXhtmlNsToXPath(document, xPath));
    }
    public static List<Node> tryGetXhtmlNodes(Document document, String xPath) {
        try {
            return getXhtmlNodes(document, xPath);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    public static List<Node> getXhtmlNodes(Node node, String xPath) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");
        return getNodes(node, addXhtmlNsToXPath(xPath));
    }
    public static List<Node> tryGetXhtmlNodes(Node node, String xPath) {
        try {
            return getXhtmlNodes(node, xPath);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * <p>
     * Get a {@link Node} from the supplied XHTML {@link Document} matching the given XPath expression. The XHTML
     * namespace prefixes are inserted automatically.
     * </p>
     *
     * @param document The {@link Document} to consider, not <code>null</code>.
     * @param xPath    The XPath expression, not <code>null</code> or empty.
     * @return Matching node for the given XPath expression, or <code>null</code> if no matching node or an error
     * occurred.
     */
    public static Node getXhtmlNode(Document document, String xPath) {
        Validate.notNull(document, "document must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");
        return getNode(document.getLastChild(), addXhtmlNsToXPath(document, xPath));
    }
    public static Node tryGetXhtmlNode(Document document, String xPath) {
        try {
            return getXhtmlNode(document, xPath);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static Node getXhtmlNode(Node node, String xPath) {
        Validate.notNull(node, "node must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");
        return getNode(node, addXhtmlNsToXPath(xPath));
    }
    public static Node tryGetXhtmlNode(Node node, String xPath) {
        try {
            return getXhtmlNode(node, xPath);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * <p>
     * Check whether {@link Document} has an XHTML namespace declared.
     * </p>
     *
     * @param document The document.
     * @return <code>true</code> if the document has an XHTML namespace declared, else <code>false</code>.
     */
    public static boolean hasXhtmlNs(Document document) {
        Validate.notNull(document, "document must not be null.");

        // added this when adding Validator.nu parser; I'm not sure,
        // whether the code below is still necessary -- Philipp, 2013-01-31
        if (XHTML_NAMESPACE.equals(document.getFirstChild().getNamespaceURI())) {
            return true;
        }

        boolean result = false;
        Node node = null;
        if (document.getLastChild() != null && document.getLastChild().getAttributes() != null) {
            node = document.getLastChild().getAttributes().getNamedItem("xmlns");
        }

        if (node != null && node.getTextContent().toLowerCase().contains("xhtml")) {
            result = true;
        }
        return result;
    }

    /**
     * <p>
     * Add the XHTML namespace to an XPath if the supplied {@link Document} is in XHTML namespace.
     * </p>
     *
     * @param document The document.
     * @param xPath    The XPath, not <code>null</code> or empty.
     * @return The XPath with the namespace.
     */
    public static String addXhtmlNsToXPath(Document document, String xPath) {
        Validate.notNull(document, "document must not be null.");
        Validate.notEmpty(xPath, "xPath must not be empty.");

        String returnValue = xPath;
        if (hasXhtmlNs(document)) {
            returnValue = addXhtmlNsToXPath(xPath);
        }
        return returnValue;
    }

    /**
     * <p>
     * Add the XHTML namespace to an XPath in case it does not have it yet.
     * </p>
     *
     * @param xPath The XPath, not <code>null</code> or empty.
     * @return The XPath with included XHTML namespace.
     */
    public static String addXhtmlNsToXPath(String xPath) {
        Validate.notEmpty(xPath, "xPath must not be empty.");

        if (xPath.toLowerCase(Locale.ENGLISH).contains(XHTML_COLON)) {
            return xPath;
        }
        // return xPath.replaceAll("/(?=\\w)","/xhtml:");
        // this is a fix NOT to touch slashes inside quotes,
        // for example in @type='application/rss+xml'
        // RegEx from http://stackoverflow.com/questions/632475/regex-to-pick-commas-outside-of-quotes
        // return xPath.replaceAll("(/)(?=\\w(?:[^']|'[^']*')*$)", "/xhtml:");
        //        return xPath.replaceAll("(/)(?=\\w+(\\[|\\/|$)(?:[^']|'[^']*')*$)", "/xhtml:");

        // The RegEx-based implementation is very error prone, therefore I changed it the following method. It splits up
        // the XPath in individual parts, and for every part we check, whether the "xhtml:" prefix needs to be added
        // (which is the case for node names like "body", "h1", ..., but not for functions like text() and logical
        // operators like "and", "or"). Similar to the RegEx-based approach, this is most likely not 100 % accurate, but
        // for tests achieved a more accurate transformation. If you discover any inaccuracies, please try to fix the
        // existing code below and add tests. Philipp, 2012-08-08

        // first we need to mask everything in quotes as this must not be analyzed for namespaces
        List<String> toMask = StringHelper.getRegexpMatches(XHTML_NS, xPath);
        int maskId = 0;
        Map<String, String> unmaskMap = new HashMap<>();
        for (String quoted : toMask) {
            String mask = "_MASK_"+(maskId++);
            xPath = xPath.replace(quoted, mask);
            unmaskMap.put(mask, quoted);
        }

        List<String> xPathParts = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        List<Character> split = Arrays.asList('/', ' ', '[', ']', '|', ')', ':');

        for (int i = 0; i < xPath.length(); i++) {
            char currentChar = xPath.charAt(i);
            if (split.contains(currentChar)) {
                xPathParts.add(buf.toString());
                buf = new StringBuilder();
                xPathParts.add(String.valueOf(currentChar));
            } else {
                buf.append(currentChar);
            }
            if (i == xPath.length() - 1) {
                xPathParts.add(buf.toString());
            }
        }

        StringBuilder result = new StringBuilder();
        for (String xPathPart : xPathParts) {
            if (XHTML_TAGGABLE.matcher(xPathPart).matches() && !AND_OR.matcher(xPathPart).matches()) {
                result.append(XHTML_COLON);
            }
            result.append(xPathPart);
        }

        String namespacedXPath = result.toString();

        // unmask again
        for (Entry<String, String> entry : unmaskMap.entrySet()) {
            namespacedXPath = namespacedXPath.replace(entry.getKey(), entry.getValue());
        }

        return namespacedXPath;
    }

}