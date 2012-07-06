package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.Annotation;

public class DuplicateTokenConsolidatorTest {
    
    private static final String SAMPLE_TEXT = "Das Reh springt hoch, das Reh springt weit. Warum auch nicht - es hat ja Zeit!";
    
    @Test
    public void testDuplicateTokenConsolidator() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new DuplicateTokenConsolidator());
        PipelineDocument<String> document = pipeline.process(new PipelineDocument<String>(SAMPLE_TEXT));
        
        List<Annotation<String>> tokenAnnotations = BaseTokenizer.getTokenAnnotations(document);
        Annotation<String> token1 = tokenAnnotations.get(0);
        assertEquals("Das", token1.getValue());
        List<Annotation<String>> duplicates1 = DuplicateTokenConsolidator.getDuplicateAnnotations(token1);
        assertEquals(1, duplicates1.size());
        assertEquals((Integer) 22, duplicates1.get(0).getStartPosition());
        assertEquals((Integer) 25, duplicates1.get(0).getEndPosition());
        assertEquals("das", duplicates1.get(0).getValue());
    }

}
