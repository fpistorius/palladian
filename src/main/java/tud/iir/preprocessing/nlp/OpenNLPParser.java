package tud.iir.preprocessing.nlp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.coref.DiscourseEntity;
import opennlp.tools.coref.LinkerMode;
import opennlp.tools.coref.mention.DefaultParse;
import opennlp.tools.coref.mention.Mention;
import opennlp.tools.lang.english.TreebankLinker;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.ConfigHolder;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

/**
 * OpenNLP Parser
 * 
 * @author Martin Wunderwald
 */
public class OpenNLPParser extends AbstractParser {

    /**
     * Logger for this class.
     */
    protected static final Logger LOGGER = Logger.getLogger(OpenNLPParser.class);

    private static final String COREF_PATH = "data/models/opennlp/coref/";

    /**
     * Identifies coreferences in an array of full parses of sentences.
     * 
     * @param parses
     *            array of full parses of sentences
     */
    public static void link(final Parse[] parses) {
        int sentenceNumber = 0;
        final List<Mention> document = new ArrayList<Mention>();

        TreebankLinker linker;
        try {
            if (DataHolder.getInstance().containsDataObject(COREF_PATH)) {
                linker = (TreebankLinker) DataHolder.getInstance().getDataObject(COREF_PATH);

            } else {

                linker = new TreebankLinker(COREF_PATH, LinkerMode.TEST);
                DataHolder.getInstance().putDataObject(COREF_PATH, linker);
            }
            final DiscourseEntity[] entities = linker.getEntities(document.toArray(new Mention[document.size()]));

            CollectionHelper.print(entities);

            for (final Parse parse : parses) {
                final DefaultParse defaultParser = new DefaultParse(parse, sentenceNumber);
                final Mention[] extents = linker.getMentionFinder().getMentions(defaultParser);

                // construct new parses for mentions which do not have
                // constituents
                for (int i = 0; i < extents.length; i++) {
                    if (extents[i].getParse() == null) {
                        final opennlp.tools.parser.Parse snp = new Parse(parse.getText(), extents[i].getSpan(), "NML",
                                1.0, i);
                        parse.insert(snp);
                        extents[i].setParse(new DefaultParse(snp, sentenceNumber));
                    }
                }

                document.addAll(Arrays.asList(extents));
                sentenceNumber++;
            }

            if (!document.isEmpty()) {
                // Mention[] ms = document.toArray(new
                // Mention[document.size()]);
                // DiscourseEntity[] entities = linker.getEntities(ms);
                // TODO return results in an appropriate data structure
                LOGGER.info(document.toString());
            }

        } catch (final IOException e) {
            LOGGER.error(e);
        }
    }

    private transient opennlp.tools.parser.Parse openNLPParse;

    /** The model path. **/
    private final transient String MODEL;

    public OpenNLPParser() {
        super();
        setName("OpenNLP Parser");
        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config == null) {
            MODEL = "";
        } else {
            MODEL = config.getString("models.root") + config.getString("models.opennlp.en.parser");
        }
    }

    /**
     * Returns the full parse for a sentence as openNLP parse.
     * 
     * @param sentence
     * @return full parse
     */
    public opennlp.tools.parser.Parse[] getFullParse(final String sentence) {

        opennlp.tools.parser.Parse[] parse = null;

        if ((opennlp.tools.parser.Parser) getModel() != null && sentence.length() > 0) {
            parse = ParserTool.parseLine(sentence, ((opennlp.tools.parser.Parser) getModel()), 1);

        }

        return parse;
    }

    /**
     * @return the most likely parse
     */
    public opennlp.tools.parser.Parse getParse() {
        return openNLPParse;
    }

    @Override
    public OpenNLPParser loadDefaultModel() {
        return loadModel(MODEL);
    }

    @Override
    public OpenNLPParser loadModel(final String configModelPath) {

        try {

            opennlp.tools.parser.Parser parser;

            if (DataHolder.getInstance().containsDataObject(configModelPath)) {
                parser = (opennlp.tools.parser.Parser) DataHolder.getInstance().getDataObject(configModelPath);

            } else {

                final StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                final InputStream modelIn = new FileInputStream(configModelPath);
                final ParserModel model = new ParserModel(modelIn);
                parser = ParserFactory.create(model);
                DataHolder.getInstance().putDataObject(configModelPath, parser);

                stopWatch.stop();
                LOGGER.info("Reading " + getName() + " from file " + configModelPath + " in "
                        + stopWatch.getElapsedTimeString());
            }

            setModel(parser);

        } catch (final IOException e) {
            LOGGER.error(e);
        }
        return this;
    }

    /**
     * Peforms a full parsing on a sentence of space-delimited tokens.
     * 
     * @param sentence
     *            the sentence
     * @return parse of the sentence or <code>null</code>, if the parser is not
     *         initialized or the sentence is empty
     */
    @Override
    public final OpenNLPParser parse(final String sentence) {

        return parse(sentence, 0);

    }

    /**
     * Persforms a full parse and selects the given index where 0 is the most
     * likely parse
     * 
     * @param sentence
     * @param index
     */
    public final OpenNLPParser parse(final String sentence, final int index) {

        openNLPParse = getFullParse(sentence)[index];

        final TagAnnotations tagAnnotations = new TagAnnotations();

        parse2Annotations(openNLPParse, tagAnnotations);

        setTagAnnotations(tagAnnotations);

        return this;
    }

    /**
     * @param parse
     *            the parse to set
     */
    public void setParse(opennlp.tools.parser.Parse parse) {
        this.openNLPParse = parse;
    }
}
