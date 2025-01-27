package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.w3c.dom.Document;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DocumentRetrieverTest {
	@Test
	public void testDocumentUriInCaseOfRedirects() {
		// will redirect to http://example.com
		String redirectingUrl = "https://httpbingo.org/redirect-to?url=http%3A%2F%2Fexample.com%2F";
		Document document = new DocumentRetriever().getWebDocument(redirectingUrl);
		assertEquals("http://example.com/", document.getDocumentURI());
	}

	@Test
	public void testGetLinks() throws ParserException {
		DocumentParser htmlParser = ParserFactory.createHtmlParser();
		Document document = htmlParser.parse(new StringInputStream("<html><body><a href=\"test.pdf\">Test 1</a><a href=\"test2.pdf\" data-pdf-title=\"A title bla bla\">Test 2</a></body></html>"));
		Set<String> links = HtmlHelper.getLinks(document, "localhost", true, true, "", true, true, new HashSet<>(Collections.singletonList("data-pdf-title")));
		assertEquals(2,  links.size());
		assertEquals(true, links.contains("test2.pdf?data-pdf-title=A+title+bla+bla"));
	}
}
