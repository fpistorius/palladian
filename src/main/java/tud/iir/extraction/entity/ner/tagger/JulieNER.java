package tud.iir.extraction.entity.ner.tagger;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.FileFormatParser;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.TaggingFormat;
import tud.iir.extraction.entity.ner.evaluation.EvaluationResult;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import de.julielab.jnet.tagger.JNETException;
import de.julielab.jnet.tagger.NETagger;
import de.julielab.jnet.tagger.Sentence;
import de.julielab.jnet.tagger.Tags;
import de.julielab.jnet.utils.Utils;

/**
 * <p>
 * This class wraps the Julie Named Entity Recognizer which uses conditional random fields.
 * </p>
 * 
 * <p>
 * The recognizer was trained on 3 bio-models on PennBioIE corpus (genes, malignancies, variation events), models are
 * available online. They are not part of the models distribution of this toolkit.
 * </p>
 * 
 * <p>
 * See also <a
 * href="http://www.julielab.de/Resources/Software/NLP+Tools/Download/Stand_alone+Tools.html">http://www.julielab
 * .de/Resources/Software/NLP+Tools/Download/Stand_alone+Tools.html</a>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class JulieNER extends NamedEntityRecognizer {

    public JulieNER() {
        setName("Julie NER");
    }

    public void demo() {
        String inputText = "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.";
        demo(inputText);
    }

    public void demo(String inputText) {
        // train
        train("data/datasets/ner/sample/trainingColumn.tsv", "data/temp/personPhoneCity.mod");

        // tag
        String taggedText = tag(inputText, "data/temp/personPhoneCity.mod.gz");
        System.out.println(taggedText);
    }

    @Override
    public String getModelFileEnding() {
        return "gz";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return true;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();

        File modelFile = new File(configModelFilePath);

        NETagger tagger = new NETagger();

        try {
            tagger.readModel(modelFile.toString());
        } catch (Exception e) {
            LOGGER.error(getName() + " error in loading model: " + e.getMessage());
            return false;
        }

        setModel(tagger);
        LOGGER.info("model " + modelFile.toString() + " successfully loaded in " + stopWatch.getElapsedTimeString());

        return true;
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        Annotations annotations = new Annotations();

        FileHelper.writeToFile("data/temp/julieInputText.txt", inputText);
        FileFormatParser.textToColumn("data/temp/julieInputText.txt", "data/temp/julieInputTextColumn.txt", " ");
        FileFormatParser.columnToSlash("data/temp/julieInputTextColumn.txt", "data/temp/julieTrainingSlash.txt", " ",
                "|");

        File testDataFile = new File("data/temp/julieTrainingSlash.txt");

        // TODO assign confidence values for predicted labels (see JNET documentation)
        boolean showSegmentConfidence = false;

        ArrayList<String> ppdTestData = Utils.readFile(testDataFile);
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();

        NETagger tagger = (NETagger) getModel();

        for (String ppdSentence : ppdTestData) {
            try {
                sentences.add(tagger.PPDtoUnits(ppdSentence));
            } catch (JNETException e) {
                LOGGER.error(getName() + " error in creating annotations: " + e.getMessage());
            }
        } // tagger.readModel(modelFile.toString());
        File outFile = new File("data/temp/juliePredictionOutput.txt");
        try {

            Utils.writeFile(outFile, tagger.predictIOB(sentences, showSegmentConfidence));

        } catch (Exception e) {
            LOGGER.error(getName() + " error in creating annotations: " + e.getMessage());
        }
        annotations = FileFormatParser.getAnnotationsFromColumn(outFile.getPath());
        CollectionHelper.print(annotations);

        return annotations;
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        loadModel(configModelFilePath);
        return getAnnotations(inputText);
    }

    /**
     * Create a file containing all entity types from the training file.
     * 
     * @param trainingFilePath
     * @return
     */
    private File createTagsFile(String trainingFilePath, String columnSeparator) {

        Set<String> tags = FileFormatParser.getTagsFromColumnFile(trainingFilePath, columnSeparator);

        StringBuilder tagsFile = new StringBuilder();
        for (String tag : tags) {
            tagsFile.append(tag).append("\n");
        }
        if (!tags.contains("O")) {
            tagsFile.append("O").append("\n");
        }

        FileHelper.writeToFile("data/temp/julieTags.txt", tagsFile);

        return new File("data/temp/julieTags.txt");
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        return train(trainingFilePath, modelFilePath, "");
    }

    public boolean train(String trainingFilePath, String modelFilePath, String configFilePath) {

        FileFormatParser.columnToSlash(trainingFilePath, "data/temp/julieTraining.txt", "\t", "|");

        File trainFile = new File("data/temp/julieTraining.txt");
        File tagsFile = createTagsFile(trainingFilePath, "\t");

        // configFilePath = "config/defaultFeatureConf.conf";

        File featureConfigFile = null;
        if (configFilePath.length() > 0) {
            featureConfigFile = new File(configFilePath);
        }

        ArrayList<String> ppdSentences = Utils.readFile(trainFile);
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();
        Tags tags = new Tags(tagsFile.toString());

        NETagger tagger;
        if (featureConfigFile != null) {
            tagger = new NETagger(featureConfigFile);
        } else {
            tagger = new NETagger();
        }
        for (String ppdSentence : ppdSentences) {
            try {
                sentences.add(tagger.PPDtoUnits(ppdSentence));
            } catch (JNETException e) {
                e.printStackTrace();
            }
        }
        tagger.train(sentences, tags);
        tagger.writeModel(modelFilePath);

        return true;
    }

    /**
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {

        JulieNER tagger = new JulieNER();

        if (args.length > 0) {

            Options options = new Options();
            options.addOption(OptionBuilder.withLongOpt("mode").withDescription("whether to tag or train a model")
                    .create());

            OptionGroup modeOptionGroup = new OptionGroup();
            modeOptionGroup.addOption(OptionBuilder.withArgName("tg").withLongOpt("tag").withDescription("tag a text")
                    .create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("tr").withLongOpt("train")
                    .withDescription("train a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("ev").withLongOpt("evaluate")
                    .withDescription("evaluate a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("dm").withLongOpt("demo")
                    .withDescription("demo mode of the tagger").create());
            modeOptionGroup.setRequired(true);
            options.addOptionGroup(modeOptionGroup);

            options.addOption(OptionBuilder.withLongOpt("trainingFile")
                    .withDescription("the path and name of the training file for the tagger (only if mode = train)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder
                    .withLongOpt("testFile")
                    .withDescription(
                            "the path and name of the test file for evaluating the tagger (only if mode = evaluate)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("configFile")
                    .withDescription("the path and name of the config file for the tagger").hasArg()
                    .withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("inputText")
                    .withDescription("the text that should be tagged (only if mode = tag)").hasArg()
                    .withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("outputFile")
                    .withDescription("the path and name of the file where the tagged text should be saved to").hasArg()
                    .withArgName("text").withType(String.class).create());

            HelpFormatter formatter = new HelpFormatter();

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption("tag")) {

                    String taggedText = tagger.tag(cmd.getOptionValue("inputText"), cmd.getOptionValue("configFile"));

                    if (cmd.hasOption("outputFile")) {
                        FileHelper.writeToFile(cmd.getOptionValue("outputFile"), taggedText);
                    } else {
                        System.out.println("No output file given so tagged text will be printed to the console:");
                        System.out.println(taggedText);
                    }

                } else if (cmd.hasOption("train")) {

                    tagger.train(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"));

                } else if (cmd.hasOption("evaluate")) {

                    EvaluationResult evResult = tagger.evaluate(cmd.getOptionValue("trainingFile"),
                            cmd.getOptionValue("configFile"), TaggingFormat.XML);
                    System.out.println(evResult);

                } else if (cmd.hasOption("demo")) {

                    tagger.demo(cmd.getOptionValue("inputText"));

                }

            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("JulieNER", options);
            }

        }

        // // HOW TO USE (some functions require the models in
        // data/models/juliener) ////
        // // train
        // tagger.train("data/datasets/ner/sample/trainingColumn.tsv", "data/temp/personPhoneCity.mod");

        // // tag
        // String taggedText = "";
        //
        // tagger.loadModel("data/temp/personPhoneCity.mod.gz");
        // taggedText = tagger
        // .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.");
        //
        // taggedText = tagger
        // .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.",
        // "data/temp/personPhoneCity.mod.gz");
        // System.out.println(taggedText);

        // // demo
        // tagger.demo();

        // // evaluate
        // System.out.println(tagger.evaluate("data/datasets/ner/sample/testingXML.xml",
        // "data/temp/personPhoneCity.mod.gz", TaggingFormat.XML));

        // /////////////////////////// train and test /////////////////////////////
        tagger.train("data/datasets/ner/politician/text/training.tsv", "data/temp/juliener.mod");
        EvaluationResult er = tagger.evaluate("data/datasets/ner/politician/text/testing.tsv",
                "data/temp/juliener.mod", TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

    }

}