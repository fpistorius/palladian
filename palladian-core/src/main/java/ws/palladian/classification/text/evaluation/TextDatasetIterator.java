package ws.palladian.classification.text.evaluation;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.ClassifiedTextDocument;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * An {@link Iterator} over {@link Dataset}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public class TextDatasetIterator implements Iterable<ClassifiedTextDocument> {

    private final List<String> fileLines;
    private final String separationString;
    private final boolean isFirstFieldLink;
    private final String datasetRootPath;

    public TextDatasetIterator(Dataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        this.fileLines = FileHelper.readFileToArray(dataset.getPath());
        this.separationString = dataset.getSeparationString();
        this.isFirstFieldLink = dataset.isFirstFieldLink();
        this.datasetRootPath = dataset.getRootPath();
    }

    public TextDatasetIterator(String filePath, String separator, boolean firstFieldLink) {
        Validate.notNull(filePath, "filePath must not be null");
        Validate.notEmpty(separator, "separator must not be empty");
        this.fileLines = FileHelper.readFileToArray(filePath);
        this.separationString = separator;
        this.isFirstFieldLink = firstFieldLink;
        this.datasetRootPath = FileHelper.getFilePath(filePath);
    }

    @Override
    public Iterator<ClassifiedTextDocument> iterator() {
        final Iterator<String> lineIterator = fileLines.iterator();
        final int totalLines = fileLines.size();
        final ProgressMonitor progressMonitor = new ProgressMonitor(totalLines, 1, "Dataset: "
                + FileHelper.getFileName(datasetRootPath));

        return new Iterator<ClassifiedTextDocument>() {

            @Override
            public boolean hasNext() {
                return lineIterator.hasNext();
            }

            @Override
            public ClassifiedTextDocument next() {
                String nextLine = lineIterator.next();
                String[] parts = nextLine.split(separationString);
                if (parts.length != 2) {
                    // XXX how to handle?
                }

                String learningText;
                if (isFirstFieldLink) {
                    learningText = FileHelper.tryReadFileToString(datasetRootPath + parts[0]);
                } else {
                    learningText = new String(parts[0]);
                }
                String instanceCategory = new String(parts[1]);
                progressMonitor.incrementAndPrintProgress();
                return new ClassifiedTextDocument(instanceCategory, learningText);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Modifications are not allowed.");
            }
        };
    }

    public static void main(String[] args) {
        String JRC_TRAIN_FILE = "/Users/pk/Dropbox/Uni/Datasets/Wikipedia76Languages/languageDocumentIndex_random1000_train.txt";
        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath(JRC_TRAIN_FILE);

        TextDatasetIterator datasetIterator = new TextDatasetIterator(dataset);
        for (Trainable trainable : datasetIterator) {
            assert (trainable != null);
        }
    }

}
