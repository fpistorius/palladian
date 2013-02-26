package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class YahooLocationExtractorTest {
    
    @Test
    public void testParse() throws Exception {
        String text = FileHelper.readFileToString(ResourceHelper.getResourceFile("testText.txt"));
        String responseJson = FileHelper.readFileToString(ResourceHelper.getResourceFile("apiResponse/yahooPlaceSpotter.json"));
        List<Annotation> annotations = YahooLocationExtractor.parseJson(text, responseJson);
        
        assertEquals(15, annotations.size());
        Annotation first = annotations.get(0);
        assertEquals("River Styx", first.getEntity());
        assertEquals(267, annotations.get(0).getOffset());
        assertEquals(277, annotations.get(0).getOffset() + annotations.get(0).getLength());
        // FeatureVector featureVector = annotations.get(0).getFeatureVector();
        // assertEquals(41.0641, featureVector.getFeature(NumericFeature.class, "latitude").getValue(), 0);
        // assertEquals(-81.8019, annotations.get(0).getFeatureVector().getFeature(NumericFeature.class, "longitude").getValue(), 0);
        // assertEquals("River Styx, Medina, OH, US", annotations.get(0).getFeatureVector().getFeature(NominalFeature.class, "name").getValue());
        // assertEquals("Suburb", annotations.get(0).getFeatureVector().getFeature(NominalFeature.class, "type").getValue());
        // assertEquals("2481927", annotations.get(0).getFeatureVector().getFeature(NominalFeature.class, "woeId").getValue());
        
        text = "The Prime Minister of Mali Cheick Modibo Diarra resigns himself and his government on television after his arrest hours earlier by leaders of the recent Malian coup d'état. (AFP via The Telegraph) (BBC) (Reuters)";
        responseJson = FileHelper.readFileToString(ResourceHelper.getResourceFile("apiResponse/yahooPlaceSpotter2.json"));
        annotations = YahooLocationExtractor.parseJson(text, responseJson);

        assertEquals(2, annotations.size());
        // assertEquals("Mali", annotations.get(0).getFeatureVector().getFeature(NominalFeature.class, "name").getValue());
        assertEquals(22, annotations.get(0).getOffset());
        // assertEquals("Mali", annotations.get(1).getFeatureVector().getFeature(NominalFeature.class, "name").getValue());
        assertEquals(153, annotations.get(1).getOffset());
    }

}
