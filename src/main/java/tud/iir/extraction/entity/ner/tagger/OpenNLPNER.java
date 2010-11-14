package tud.iir.extraction.entity.ner.tagger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import opennlp.maxent.EventStream;
import opennlp.maxent.GISModel;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.io.PooledGISModelReader;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.tools.lang.english.NameFinder;
import opennlp.tools.namefind.NameFinderEventStream;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import tud.iir.classification.page.evaluation.Dataset;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.FileFormatParser;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.TaggingFormat;
import tud.iir.extraction.entity.ner.evaluation.EvaluationResult;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;

/**
 * <p>
 * This class wraps the OpenNLP Named Entity Recognizer which uses a maximum entropy approach.
 * </p>
 * 
 * <p>
 * The following models exist already for this recognizer:
 * <ul>
 * <li>Date</li>
 * <li>Location</li>
 * <li>Money</li>
 * <li>Organization</li>
 * <li>Percentage</li>
 * <li>Person</li>
 * <li>Time</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Changes to the original OpenNLP code:
 * <ul>
 * <li>made nameFinder public in NameFinder.java</li>
 * <li>NameSampleDataStream.java added lines 43 to 46 to allow non white-spaced tagging</li>
 * <li>the model names must have the following format openNLP_TAG.bin.gz where "TAG" is the name of the tag that will be
 * tagged by this model</li>
 * </ul>
 * </p>
 * 
 * <p>
 * See also <a
 * href="http://sourceforge.net/apps/mediawiki/opennlp/index.php?title=Name_Finder#Named_Entity_Annotation_Guidelines"
 * >http://sourceforge.net/apps/mediawiki/opennlp/index.php?title=Name_Finder#Named_Entity_Annotation_Guidelines</a>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class OpenNLPNER extends NamedEntityRecognizer {

    private NameFinder[] nameFinders;

    public OpenNLPNER() {
        setName("OpenNLP NER");
    }

    public void demo() {
        String inputText = "Microsoft Inc. is a company which was founded by Bill Gates many years ago. The company's headquarters are close to Seattle in the USA.";
        demo(inputText);
    }

    public void demo(String inputText) {
        System.out
                .println(tag(
                        inputText,
                        "data/models/opennlp/openNLP_organization.bin.gz,data/models/opennlp/openNLP_person.bin.gz,data/models/opennlp/openNLP_location.bin.gz"));
    }

    /**
     * Adds tags to the given text using the name entity models.
     * 
     * @param finders The name finders to be used.
     * @param tags The tag names for the corresponding name finder.
     * @param input The input reader.
     * @return A tagged string.
     * @throws IOException
     */
    private StringBuilder processText(NameFinder[] finders, String[] tags, String text) throws IOException {

        // the names of the tags
        Span[][] nameSpans = new Span[finders.length][];
        String[][] nameOutcomes = new String[finders.length][];
        Tokenizer tokenizer = new SimpleTokenizer();

        StringBuilder output = new StringBuilder();

        if (text.equals("")) {
            return new StringBuilder();
        }

        output.setLength(0);
        Span[] spans = tokenizer.tokenizePos(text);
        String[] tokens = Span.spansToStrings(spans, text);

        // let each model (one for each concept) tag the text
        for (int fi = 0, fl = finders.length; fi < fl; fi++) {
            nameSpans[fi] = finders[fi].nameFinder.find(tokens);
            nameOutcomes[fi] = NameFinderEventStream.generateOutcomes(nameSpans[fi], null, tokens.length);
        }

        for (int ti = 0, tl = tokens.length; ti < tl; ti++) {
            for (int fi = 0, fl = finders.length; fi < fl; fi++) {
                // check for end tags
                if (ti != 0) {
                    if ((nameOutcomes[fi][ti].equals(NameFinderME.START) || nameOutcomes[fi][ti]
                            .equals(NameFinderME.OTHER))
                            && (nameOutcomes[fi][ti - 1].equals(NameFinderME.START) || nameOutcomes[fi][ti - 1]
                                    .equals(NameFinderME.CONTINUE))) {
                        output.append("</").append(tags[fi]).append(">");
                    }
                }
            }
            if (ti > 0 && spans[ti - 1].getEnd() < spans[ti].getStart()) {
                output.append(text.substring(spans[ti - 1].getEnd(), spans[ti].getStart()));
            }
            // check for start tags
            for (int fi = 0, fl = finders.length; fi < fl; fi++) {
                if (nameOutcomes[fi][ti].equals(NameFinderME.START)) {
                    output.append("<").append(tags[fi]).append(">");
                }
            }
            output.append(tokens[ti]);
        }
        // final end tags
        if (tokens.length != 0) {
            for (int fi = 0, fl = finders.length; fi < fl; fi++) {
                if (nameOutcomes[fi][tokens.length - 1].equals(NameFinderME.START)
                        || nameOutcomes[fi][tokens.length - 1].equals(NameFinderME.CONTINUE)) {
                    output.append("</").append(tags[fi]).append(">");
                }
            }
        }
        if (tokens.length != 0) {
            if (spans[tokens.length - 1].getEnd() < text.length()) {
                output.append(text.substring(spans[tokens.length - 1].getEnd()));
            }
        }

        return output;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {

        StopWatch stopWatch = new StopWatch();

        String[] modelFilePaths = configModelFilePath.split(",");

        NameFinder[] finders = new NameFinder[modelFilePaths.length];
        String[] tags = new String[finders.length];

        for (int finderIndex = 0; finderIndex < modelFilePaths.length; finderIndex++) {

            String modelName = modelFilePaths[finderIndex];
            String tagName = modelName;
            int tagStartIndex = modelName.lastIndexOf("_");
            if (tagStartIndex > -1) {
                tagName = modelName.substring(tagStartIndex + 1, modelName.indexOf(".", tagStartIndex));
            } else {
                LOGGER.warn("model name does not comply \"openNLP_TAG.bin.gz\" format");
            }

            try {
                finders[finderIndex] = new NameFinder(new PooledGISModelReader(new File(modelName)).getModel());
            } catch (IOException e) {
                LOGGER.error(getName() + " error in loading model: " + e.getMessage());
                return false;
            }
            tags[finderIndex] = tagName.toUpperCase();
        }

        Object[] objs = new Object[2];
        objs[0] = finders;
        objs[1] = tags;

        setModel(objs);
        LOGGER.info("model " + configModelFilePath + " successfully loaded in " + stopWatch.getElapsedTimeString());

        return true;
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        Annotations annotations = new Annotations();

        Object[] objs = (Object[]) getModel();
        NameFinder[] finders = (NameFinder[]) objs[0];
        String[] tags = (String[]) objs[1];

        String taggedText = "";
        try {
            taggedText = processText(finders, tags, inputText).toString();
        } catch (IOException e) {
            LOGGER.error("could not tag text with " + getName() + ", " + e.getMessage());
        }

        String taggedTextFilePath = "data/temp/taggedText.txt";
        FileHelper.writeToFile(taggedTextFilePath, taggedText);

        annotations = FileFormatParser.getAnnotationsFromXMLFile(taggedTextFilePath);

        // CollectionHelper.print(annotations);

        return annotations;
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {

        loadModel(configModelFilePath);
        return getAnnotations(inputText);
    }

    @Override
    public String getModelFileEnding() {
        return "bin.gz";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        File inFile = null;
        File outFile = null;

        inFile = new File(trainingFilePath);
        outFile = new File(modelFilePath);

        int iterations = 100;
        int cutoff = 5;

        GISModel mod;
        EventStream es;

        try {
            es = new NameFinderEventStream(new NameSampleDataStream(new PlainTextByLineDataStream(
                    new FileReader(inFile))));

            mod = NameFinderME.train(es, iterations, cutoff);
            LOGGER.info("saving the model as: " + outFile.toString());

            new SuffixSensitiveGISModelWriter(mod, outFile).persist();

        } catch (FileNotFoundException e) {
            LOGGER.error("error during training model in " + getName() + ", with " + trainingFilePath + " and "
                    + modelFilePath + ", " + e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.error("error during training model in " + getName() + ", with " + trainingFilePath + " and "
                    + modelFilePath + ", " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {

        OpenNLPNER tagger = new OpenNLPNER();

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
                formatter.printHelp("OpenNLPNER", options);
            }

        }

        // // HOW TO USE (some functions require the models in
        // data/models/opennlp) ////
        // // train
        // tagger.train("data/datasets/ner/sample/trainingPhoneXML.xml",
        // "data/models/opennlp/openNLP_phone.bin.gz");

        // // tag
        // String taggedText = tagger
        // .tag(
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone. The iphone 4 is modern. Seattle is a rainy city.",
        // "data/models/opennlp/openNLP_person.bin.gz,data/models/opennlp/openNLP_location.bin.gz,data/models/opennlp/openNLP_phone.bin.gz");
        // System.out.println(taggedText);

        // // demo
        // tagger.demo();

        // // evaluate
        // System.out
        // .println(
        // tagger
        // .evaluate(
        // "data/datasets/ner/sample/testingXML.xml",
        // "data/models/opennlp/openNLP_organization.bin.gz,data/models/opennlp/openNLP_person.bin.gz,data/models/opennlp/openNLP_location.bin.gz",
        // TaggingFormat.XML));

        // TODO one model per concept
        Dataset trainingDataset = new Dataset();
        trainingDataset.setPath("data/datasets/ner/www_test/index_split1.txt");
        tagger.train(trainingDataset, "data/temp/opennlp.bin.gz");

        Dataset testingDataset = new Dataset();
        testingDataset.setPath("data/datasets/ner/www_test/index_split2.txt");
        EvaluationResult er = tagger.evaluate(testingDataset, "data/temp/opennlp.bin.gz");
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

    }

}
