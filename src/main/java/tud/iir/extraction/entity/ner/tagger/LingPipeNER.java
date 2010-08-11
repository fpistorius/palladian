package tud.iir.extraction.entity.ner.tagger;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import ner.lingpipe.Conll2002ChunkTagParser;
import ner.lingpipe.FileScorer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import tud.iir.extraction.entity.ner.Annotation;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.FileFormatParser;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.TaggingFormat;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;

import com.aliasi.chunk.AbstractCharLmRescoringChunker;
import com.aliasi.chunk.CharLmRescoringChunker;
import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.ChunkerEvaluator;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.NBestChunker;
import com.aliasi.corpus.Parser;
import com.aliasi.corpus.parsers.Muc6ChunkParser;
import com.aliasi.lm.LanguageModel.Process;
import com.aliasi.lm.LanguageModel.Sequence;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;


/**
 * <p>
 * This class wraps the LingPipe implementation of a Named Entity Recognizer.
 * </p>
 * 
 * <p>
 * See also <a
 * href="http://alias-i.com/lingpipe/demos/tutorial/ne/read-me.html">http://alias-i.com/lingpipe/demos/tutorial
 * /ne/read-me.html</a>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class LingPipeNER extends NamedEntityRecognizer {

    private static final int NUM_CHUNKINGS_RESCORED = 64;
    private static final int MAX_N_GRAM = 8;
    private static final int NUM_CHARS = 256;
    private static final double LM_INTERPOLATION = MAX_N_GRAM;

    public LingPipeNER() {
        setName("LingPipe NER");
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        try {
            FileFormatParser ffp = new FileFormatParser();
            String trainingFilePath2 = trainingFilePath.replaceAll("\\.", "_tranformed.");
            ffp.tsvToSsv(trainingFilePath, trainingFilePath2);

            File corpusFile = new File(trainingFilePath2);
            File modelFile = new File(modelFilePath);
            // File devFile = new File(developmentFilePath);

            LOGGER.info("setting up Chunker Estimator");
            TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;
            CharLmRescoringChunker chunkerEstimator = new CharLmRescoringChunker(factory, NUM_CHUNKINGS_RESCORED,
                    MAX_N_GRAM, NUM_CHARS, LM_INTERPOLATION);
            // HmmCharLmEstimator hmmEstimator = new HmmCharLmEstimator(MAX_N_GRAM, NUM_CHARS, LM_INTERPOLATION);
            // CharLmHmmChunker chunkerEstimator = new CharLmHmmChunker(factory, hmmEstimator);

            LOGGER.info("setting up Data Parser");
            // GeneTagParser parser = new GeneTagParser();
            Conll2002ChunkTagParser parser = new Conll2002ChunkTagParser();
            parser.setHandler(chunkerEstimator);

            LOGGER.info("training with data from file=" + corpusFile);
            parser.parse(corpusFile);

            // System.out.println("Training with Data from File=" + devFile);
            // parser.parse(devFile);

            LOGGER.info("compiling and writing model to file=" + modelFile);
            AbstractExternalizable.compileTo(chunkerEstimator, modelFile);

        } catch (IOException e) {
            LOGGER.error(getName() + " failed training, " + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {

        Annotations annotations = new Annotations();

        try {

            File modelFile = new File(configModelFilePath);

            LOGGER.info("Reading chunker from file=" + modelFile);
            Chunker chunker = (Chunker) AbstractExternalizable.readObject(modelFile);

            String[] args = new String[1];
            args[0] = inputText;
            Set<Chunk> chunkSet = new HashSet<Chunk>();
            for (int i = 0; i < args.length; ++i) {
                Chunking chunking = chunker.chunk(args[i]);
                LOGGER.debug("Chunking=" + chunking);
                chunkSet.addAll(chunking.chunkSet());
            }

            for (Chunk chunk : chunkSet) {
                int offset = chunk.start();
                Entity namedEntity = new Entity("???", new Concept(chunk.type()));

                Annotation annotation = new Annotation(offset, namedEntity.getName(), namedEntity.getConcept()
                        .getName());
                annotations.add(annotation);
            }

        } catch (IOException e) {
            LOGGER.error(getName() + " could not tag input, " + e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.error(getName() + " could not tag input, " + e.getMessage());
        }

        CollectionHelper.print(annotations);

        return annotations;
    }

    // java TrainGeneTag <trainingInputFile> <modelOutputFile>
    @Deprecated
    public void trainNER(String trainingFilePath, String developmentFilePath, String modelOutputFilePath)
            throws IOException {

        FileFormatParser ffp = new FileFormatParser();
        String trainingFilePath2 = trainingFilePath.replaceAll("\\.", "_tranformed.");
        ffp.tsvToSsv(trainingFilePath, trainingFilePath2);

        File corpusFile = new File(trainingFilePath2);
        File modelFile = new File(modelOutputFilePath);
        File devFile = new File(developmentFilePath);

        System.out.println("Setting up Chunker Estimator");
        TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;
        CharLmRescoringChunker chunkerEstimator = new CharLmRescoringChunker(factory, NUM_CHUNKINGS_RESCORED,
                MAX_N_GRAM, NUM_CHARS, LM_INTERPOLATION);
        // HmmCharLmEstimator hmmEstimator = new HmmCharLmEstimator(MAX_N_GRAM, NUM_CHARS, LM_INTERPOLATION);
        // CharLmHmmChunker chunkerEstimator = new CharLmHmmChunker(factory, hmmEstimator);

        System.out.println("Setting up Data Parser");
        // GeneTagParser parser = new GeneTagParser();
        Conll2002ChunkTagParser parser = new Conll2002ChunkTagParser();
        parser.setHandler(chunkerEstimator);

        System.out.println("Training with Data from File=" + corpusFile);
        parser.parse(corpusFile);

        System.out.println("Training with Data from File=" + devFile);
        // parser.parse(devFile);

        System.out.println("Compiling and Writing Model to File=" + modelFile);
        AbstractExternalizable.compileTo(chunkerEstimator, modelFile);
    }

    public void evaluateNER(String modelFilePath, String testFilePath) throws Exception {

        File chunkerFile = new File(modelFilePath);
        File testFile = new File(testFilePath);

        @SuppressWarnings("rawtypes")
        AbstractCharLmRescoringChunker<NBestChunker, Process, Sequence> chunker = (AbstractCharLmRescoringChunker) AbstractExternalizable
                .readObject(chunkerFile);

        ChunkerEvaluator evaluator = new ChunkerEvaluator(chunker);
        evaluator.setVerbose(true);

        Conll2002ChunkTagParser parser = new Conll2002ChunkTagParser();
        parser.setHandler(evaluator);

        parser.parse(testFile);

        System.out.println(evaluator.toString());
    }

    public void scoreNER(String[] args) throws IOException {
        File refFile = new File(args[0]);
        File responseFile = new File(args[1]);

        Parser parser = new Muc6ChunkParser();
        FileScorer scorer = new FileScorer(parser);
        scorer.score(refFile, responseFile);

        System.out.println(scorer.evaluation().toString());
    }

    @Deprecated
    public void useLearnedNER(String modelFilePath, String inputText) throws IOException, ClassNotFoundException {

        File modelFile = new File(modelFilePath);

        System.out.println("Reading chunker from file=" + modelFile);
        Chunker chunker = (Chunker) AbstractExternalizable.readObject(modelFile);

        Annotations annotations = new Annotations();

        String[] args = new String[1];
        args[0] = inputText;
        Set<Chunk> chunkSet = new HashSet<Chunk>();
        for (int i = 0; i < args.length; ++i) {
            Chunking chunking = chunker.chunk(args[i]);
            System.out.println("Chunking=" + chunking);
            chunkSet.addAll(chunking.chunkSet());
        }

        for (Chunk chunk : chunkSet) {
            int offset = chunk.start();
            Entity namedEntity = new Entity("???", new Concept(chunk.type()));

            Annotation annotation = new Annotation(offset, namedEntity.getName(), namedEntity.getConcept().getName());
            annotations.add(annotation);
        }

        CollectionHelper.print(annotations);
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        LingPipeNER tagger = new LingPipeNER();

        // learn
        // lpt.trainNER("data/temp/esp.train", "data/temp/esp.testa", "data/temp/ne-esp-muc6.model");

        // lpt.trainNER("data/temp/stanfordner/example/jane-austen-emma-ch1.tsv",
        // "","data/temp/ne-en-janeausten-lp.model");

        // lpt.trainNER("data/temp/allColumnBIO.tsv", "", "data/temp/ne-en-mobilephone-lp.model");

        // use
        // lpt.useLearnedNER("data/temp/ne-en-news-muc6.AbstractCharLmRescoringChunker",
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle.");
        // lpt.useLearnedNER("data/temp/ne-esp-muc6.model",
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle.");

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

                    tagger.evaluate(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"),
                            TaggingFormat.XML);

                }


            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("FeedChecker", options);
            }

        }

        // // HOW TO USE ////
        // tagger.tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.",
        // "data/temp/ne-en-mobilephone-lp.model")
        //
        // tagger.useLearnedNER(
        // "data/temp/ne-en-mobilephone-lp.model",
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.");
        // evaluate
        // lpt.evaluateNER("data/temp/ne-esp-muc6.model", "data/temp/esp.testb");

    }

}
