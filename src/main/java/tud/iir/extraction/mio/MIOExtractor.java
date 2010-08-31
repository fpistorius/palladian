/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;

import tud.iir.classification.mio.MIOClassifier;
import tud.iir.extraction.Extractor;
import tud.iir.helper.DateHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.persistence.DatabaseManager;

public final class MIOExtractor extends Extractor {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(MIOExtractor.class);

    /** The instance. */
    private static MIOExtractor instance = null;

    /** The Constant MAX_EXTRACTION_THREADS. */
    private static final int MAX_EXTRACTION_THREADS = 3;

    /**
     * Instantiates a new MIOExtractor.
     */
    private MIOExtractor() {
        // super();
        addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);
        addSuffixesToBlackList(Extractor.URL_TEXTUAL_BLACKLIST);
    }

    /**
     * Gets the single instance of MIOExtractor.
     * 
     * @return single instance of MIOExtractor
     */
    public static MIOExtractor getInstance() {
        if (instance == null) {
            instance = new MIOExtractor();
        }
        return instance;
    }

    /**
     * Start extraction of MIOs for entities that are fetched from the knowledge base. Continue from last extraction.
     */
    public void startExtraction() {
        startExtraction(true);
    }

    /**
     * Start extraction.
     * 
     * @param continueFromLastExtraction the continue from last extraction
     */
    public void startExtraction(final boolean continueFromLastExtraction) {
        
    
//        MIOClassifier mioClass = new MIOClassifier();
//        if (!mioClass.doesTrainedMIOClassifierExists()){
//            mioClass.trainClassifier("data/miofeatures_scored.txt");
//            mioClass.saveTrainedClassifier();
//        }


        LOGGER.info("start MIO extraction");

        // System.exit(1);

        // reset stopped command
        setStopped(false);

        // load concepts and attributes from ontology (and rdb)
        final KnowledgeManager kManager = DatabaseManager.getInstance().loadOntology();
        setKnowledgeManager(kManager);

        // loop until exit called
        // while (!isStopped()) {

        // concepts
        final ArrayList<Concept> concepts = knowledgeManager.getConcepts(true);
        // final ArrayList<Concept> concepts = DatabaseManager.getInstance().loadConcepts();

        // loadInCoFiConfiguration and prepare as Singleton
        final InCoFiConfiguration configuration = loadConfiguration();
        InCoFiConfiguration.instance = configuration;

        // System.out.println(InCoFiConfiguration.getInstance().getVocByConceptName("car").toString());
        // System.out.println(InCoFiConfiguration.getInstance().getWeakInteractionIndicators().toString());
        // System.exit(1);

        // iterate through all concepts
        for (Concept currentConcept : concepts) {

            if (currentConcept.getName().toLowerCase().contains("movie")) {
                // load Entities from DB for current concept
                currentConcept.loadEntities(false);
                // }

                if (isStopped()) {
                    LOGGER.info("mio extraction process stopped");
                    // Clean the SWF-File-DownloadDirectory

                    // FileHelper.cleanDirectory( InCoFiConfiguration.getInstance().tempDirPath);
                    break;
                }

                // iterate through all entities of current concept
                ArrayList<Entity> conceptEntities;
                if (continueFromLastExtraction) {
                    conceptEntities = currentConcept.getEntitiesByDate();
                } else {
                    conceptEntities = currentConcept.getEntities();
                }

                // wait for a certain time when no entities were found, then
                // restart
                if (conceptEntities.isEmpty()) {
                    LOGGER.info("no entities for mio extraction, continue with next concept");
                    continue;
                }

                final ThreadGroup extractionThreadGroup = new ThreadGroup("mioExtractionThreadGroup");

                for (Entity currentEntity : conceptEntities) {

//                    if (currentEntity.getName().toLowerCase(Locale.ENGLISH).contains("avant")) {
                        if (isStopped()) {
                            LOGGER.info("mio extraction process stopped");
                            break;
                        }

                        currentEntity.setLastSearched(new Date(System.currentTimeMillis()));

                        LOGGER.info("  start mio extraction process for entity \"" + currentEntity.getName() + "\" ("
                                + currentEntity.getConcept().getName() + ")");
                        final Thread mioThread = new EntityMIOExtractionThread(extractionThreadGroup,
                                currentEntity.getSafeName() + "MIOExtractionThread", currentEntity,
                                getKnowledgeManager());
                        mioThread.start();

                        LOGGER.info("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());
                        // System.out.println("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());

                        int count = 0;
                        while (getThreadCount() >= MAX_EXTRACTION_THREADS) {
                            LOGGER.info("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ") "
                                    + extractionThreadGroup.activeCount() + ","
                                    + extractionThreadGroup.activeGroupCount());
                            if (extractionThreadGroup.activeCount() + extractionThreadGroup.activeGroupCount() == 0) {
                                LOGGER.warn("apparently "
                                        + getThreadCount()
                                        + " threads have not finished correctly but thread group is empty, continuing...");
                                resetThreadCount();
                                break;
                            }
                            // System.out.println("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ")");
                            ThreadHelper.sleep(WAIT_FOR_FREE_THREAD_SLOT);
                            if (isStopped()) {
                                count++;
                            }
                            if (count > 1) {
                                LOGGER.info("waited 25 iterations after stop has been called, breaking now");
                                break;
                            }
                        }

                    }

                }


        }
  

    }

    /**
     * Save the extractionResults to database.
     * 
     * @param saveResults the save results
     */
    @Override
    protected void saveExtractions(final boolean saveResults) {
        if (saveResults) {
            LOGGER.info("..Saving MIOExtractionResults");
            getKnowledgeManager().saveExtractions();
        }
    }

    /**
     * load the concept-specific SearchVocabulary from .yml-file
     * 
     * @return the concept search vocabulary
     */
    private InCoFiConfiguration loadConfiguration() {
        InCoFiConfiguration returnValue = null;
        try {
            final InCoFiConfiguration config = Yaml.loadType(new File("config/inCoFiConfigurations.yml"),
                    InCoFiConfiguration.class);

            returnValue = config;
        } catch (FileNotFoundException e) {

            LOGGER.error(e.getMessage());
        }
        return returnValue;

    }

    /**
     * Check if URL is allowed.
     * 
     * @param url the URL
     * @return true, if the URL allowed
     */
    public boolean isURLallowed(final String url) {
        return super.isURLallowed(url);
    }

    /**
     * The main method.
     * 
     * @param abc the arguments
     */
    public static void main(final String[] abc) {
        // Controller.getInstance();

        // long t1 = System.currentTimeMillis();
        final MIOExtractor mioEx = MIOExtractor.getInstance();

        mioEx.startExtraction(false);
        // mioEx.stopExtraction(true);

        // DateHelper.getRuntime(t1, true);
    }

}
