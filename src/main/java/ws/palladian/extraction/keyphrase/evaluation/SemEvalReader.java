package ws.palladian.extraction.keyphrase.evaluation;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ws.palladian.extraction.keyphrase.extractors.PalladianKeyphraseExtractor;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.LineAction;

public class SemEvalReader {
    
    private static final String PATH = "/Users/pk/Desktop/SemEval2010/train";
    
    public static void main(String[] args) {
        
        PalladianKeyphraseExtractor extractor = new PalladianKeyphraseExtractor();
        
        final Map<String,Set<String>> keyphrases = new HashMap<String,Set<String>>();
        final Map<String,String> documents = new HashMap<String, String>();
        String assignedKeyphrasesFile = PATH + "/" + "train.reader.stem.final";
        FileHelper.performActionOnEveryLine(assignedKeyphrasesFile, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split(" : ");
                Set<String> k = new HashSet<String>();
                k.addAll(Arrays.asList(split[1].split(",")));
                keyphrases.put(split[0],k);
            }
        });
        File[] files = FileHelper.getFiles(PATH, ".txt.final");
        for (File file : files) {
            String fileId = file.getName().replace(".txt.final", "");
            String content = FileHelper.readFileToString(file);
            documents.put(fileId, content);
        }
        
        Set<Entry<String, String>> documentsSet = documents.entrySet();
        System.out.println("# total docs " + documentsSet.size());
        int counter = 0;
        for (Entry<String, String> entry : documentsSet) {
            String documentId = entry.getKey();
            String content = entry.getValue();
            Set<String> assignedKeyphrases = keyphrases.get(documentId);
            System.out.println("docId:" + documentId);
//            System.out.println("content: " + content);
//            System.out.println("keyphrases: " + assignedKeyphrases);
//            System.out.println("=======");
            extractor.train(content, assignedKeyphrases, 0);
            System.out.println("finished: " + (float)counter++/documentsSet.size());
        }
        
        extractor.endTraining();
        
    }

}
