package ws.palladian.extraction.entity;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Named Entity Recognizers (NER) of this abstract class are can can be trained on input data.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public abstract class TrainableNamedEntityRecognizer extends NamedEntityRecognizer {

    /**
     * The file ending of the model file.
     * 
     * @return The file ending of the model/config file.
     */
    public abstract String getModelFileEnding();

    public String getModelFileEndingIfNotSetAutomatically() {
        if (setsModelFileEndingAutomatically()) {
            return "";
        }

        return getModelFileEnding();
    }

    /**
     * Whether or not the NER sets the model file ending itself after specifying the model name.
     * 
     * @return True, if it does, false otherwise.
     */
    public abstract boolean setsModelFileEndingAutomatically();

    /**
     * Whether the NER needs one model file per concept. Usually you can train and recognize several entities using only
     * one model.
     * 
     * @return True, if you need to train each concept separately, false otherwise.
     */
    public boolean oneModelPerConcept() {
        return false;
    }

    public abstract boolean loadModel(String configModelFilePath);

    /**
     * <p>
     * Train the named entity recognizer using the data from the training file and save it to the model file path.
     * </p>
     * <p>
     * The training file must be given in tab (\t) separated column format where the first column is the term and the
     * second column is the concept.
     * </p>
     * 
     * @param trainingFilePath The path where the training data can be found.
     * @param modelFilePath The path where the trained model should be saved to.
     * @return True, if the training succeeded, false otherwise.
     */
    public abstract boolean train(String trainingFilePath, String modelFilePath);

    public boolean train(File trainingFile, File modelFile) {
        return train(trainingFile.getPath(), modelFile.getPath());
    }

    public boolean train(Dataset dataset, String modelFilePath) {

        if (dataset.isColumnNER()) {
            return train(dataset.getPath(), modelFilePath);
        }

        String tempFilePath = "data/temp/nerConcatenated.xml";
        String tempColumnFilePath = FileHelper.appendToFileName(tempFilePath, "_tsv");

        // delete temp file that might have been created
        FileHelper.delete(tempFilePath);
        FileHelper.delete(tempColumnFilePath);

        if (!oneModelPerConcept()) {

            // concatenate all xml files from the training index to one large file
            List<String> lines = FileHelper.readFileToArray(dataset.getPath());
            for (String line : lines) {

                String[] parts = line.split(" ");

                FileHelper.concatenateFiles(new File(tempFilePath), new File(dataset.getRootPath() + parts[0]));
            }

            // transform file to tsv format
            FileFormatParser.xmlToColumn(tempFilePath, tempColumnFilePath, "\t");

            return train(tempColumnFilePath, modelFilePath);

        } else {

            boolean trainingComplete = false;

            // map containing the parts with the file links
            Map<String, Set<String>> conceptMap = new HashMap<String, Set<String>>();

            List<String> lines = FileHelper.readFileToArray(dataset.getPath());
            for (String line : lines) {

                if (line.length() == 0) {
                    continue;
                }

                String[] lineParts = line.split(" ");
                String part = lineParts[1].replaceAll("_part(\\d+)", "");

                Set<String> links = conceptMap.get(part);
                if (links == null) {
                    links = new HashSet<String>();
                    links.add(lineParts[0]);
                    conceptMap.put(part, links);
                } else {
                    links.add(lineParts[0]);
                }
            }

            // train x files where x is the number of concepts
            for (Entry<String, Set<String>> partEntry : conceptMap.entrySet()) {

                // concatenate all files for this current concept
                for (String link : partEntry.getValue()) {
                    String[] parts = link.split(" ");

                    FileHelper.concatenateFiles(new File(tempFilePath), new File(dataset.getRootPath() + parts[0]));
                }

                // transform file to tsv format
                FileFormatParser.xmlToColumn(tempFilePath, tempColumnFilePath, "\t");

                trainingComplete = train(tempColumnFilePath,
                        FileHelper.appendToFileName(modelFilePath, "_" + partEntry.getKey().toUpperCase()));

                if (!trainingComplete) {
                    return false;
                }

            }

            return trainingComplete;
        }
    }

    // /**
    // * Get the tags that were trained for this model.
    // *
    // * @param modelPath The path to the model
    // * @return A list of tags that the model can apply to text.
    // */
    // public List<String> getModelTags(String modelPath) {
    // String filename = FileHelper.getFileName(modelPath);
    // String path = FileHelper.getFilePath(modelPath);
    // filename += "_meta.txt";
    // return FileHelper.readFileToArray(path + filename);
    // }

    // /**
    // * Tag the input text using the given model or model configuration.
    // *
    // * @param inputText The text to be tagged.
    // * @param configModelFilePath The file to the model or the configuration depending on the NER. Every NER has it's
    // * own model or configuration file.
    // * @return The tagged string in the specified {@link TaggingFormat}.
    // */
    // public String tag(String inputText, String configModelFilePath) {
    // if (configModelFilePath.length() > 0) {
    // loadModel(configModelFilePath);
    // }
    // return tag(inputText);
    // }

    // public String tag(File inputFile, File configModelFile) {
    // String inputText = FileHelper.readFileToString(inputFile.getPath());
    // return tag(inputText, configModelFile.getPath());
    // }

    // public void tag(String inputText, String outputFilePath, String configModelFilePath) {
    // String taggedText = tag(inputText, configModelFilePath);
    // FileHelper.writeToFile(outputFilePath, taggedText);
    // }

    // public void tag(File inputFile, File outputFile, File configModelFile) {
    // String inputText = FileHelper.readFileToString(inputFile.getPath());
    // tag(inputText, outputFile.getPath(), configModelFile.getPath());
    // }

}