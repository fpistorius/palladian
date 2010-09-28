/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tud.iir.knowledge.Entity;

/**
 * The Class UniversalMIOExtractor is a context-based MIO-Extractor.
 * 
 */
public class UniversalMIOExtractor {

    final Entity entity;

    public UniversalMIOExtractor(final Entity entity) {
        this.entity = entity;
    }

    public List<MIO> analyzeMIOPages(final List<MIOPage> mioPages) {
        List<MIO> mios = new ArrayList<MIO>();

        for (MIOPage mioPage : mioPages) {

            // extract MIOs and calculate features
            mios.addAll(extractAllMIOs(mioPage));

        }

//        System.out.println("Anzahl MIOs vor DuplicateRemoval: " + mios.size());
        mios = removeMIODuplicates(mios);
//        System.out.println("Anzahl MIOs nach DuplicateRemoval: " + mios.size());

        return mios;
    }

    /**
     * Extract all MIOs.
     * 
     * @param mioPage the mioPage
     * @param entity the entity
     * @return the list
     */
    private List<MIO> extractAllMIOs(final MIOPage mioPage) {

        final List<String> relevantMIOTypes = InCoFiConfiguration.getInstance().getMIOTypes();
        List<MIO> mioList = new ArrayList<MIO>();

        if (relevantMIOTypes.contains("flash")) {
            final FlashExtractor flashExtractor = new FlashExtractor();
            mioList.addAll(flashExtractor.extractMIOsByType(mioPage, entity));
        }

        if (relevantMIOTypes.contains("silverlight")) {
            final SilverlightExtractor slExtractor = new SilverlightExtractor();
            mioList.addAll(slExtractor.extractMIOsByType(mioPage, entity));
        }

        if (relevantMIOTypes.contains("applet")) {
            final AppletExtractor appletExtractor = new AppletExtractor();
            mioList.addAll(appletExtractor.extractMIOsByType(mioPage, entity));
        }

        if (relevantMIOTypes.contains("quicktime")) {
            final QuicktimeExtractor qtExtractor = new QuicktimeExtractor();
            mioList.addAll(qtExtractor.extractMIOsByType(mioPage, entity));

        }
        if (relevantMIOTypes.contains("html5canvas")) {
            final HTML5CanvasExtractor canvasExtractor = new HTML5CanvasExtractor();
            mioList.addAll(canvasExtractor.extractMIOsByType(mioPage, entity));
        }

        // Calculate Features and Interactivity
        mioList = calcFeaturesAndInteractivity(mioList, mioPage, entity);

        return mioList;
    }

    /**
     * Removes the MIO-Duplicates.
     * 
     * @param mioList the list of MIOs
     * @return the list without duplicates
     */
    private List<MIO> removeMIODuplicates(final List<MIO> mioList) {

        final List<MIO> resultList = new ArrayList<MIO>();
        final Map<String, MIO> mioMap = new HashMap<String, MIO>();

        for (MIO mio : mioList) {
            if (mioMap.containsKey(mio.getDirectURL())) {

                final MIO existingMio = mioMap.get(mio.getDirectURL());

                MIO mergedMIO = mergeMIOs(existingMio, mio);
                mioMap.put(mergedMIO.getDirectURL(), mergedMIO);

            } else {

                String testMioDirectURL = mio.getDirectURL();
                boolean isContained = false;
                MIO mergedMIO = null;
                String existingMIODirectURL = "";
                String removalMIOURL = "";
                for (Entry<String, MIO> mioEntry : mioMap.entrySet()) {
                    MIO existingMIO = mioEntry.getValue();
                    existingMIODirectURL = existingMIO.getDirectURL();

                    // check if a shorter version of the directURL is already existing
                    if (existingMIODirectURL.contains(testMioDirectURL)) {
                        mergedMIO = mergeMIOs(mio, existingMIO);
                        isContained = true;
                        removalMIOURL = testMioDirectURL;
                        break;
                    } else {
                        if (testMioDirectURL.contains(existingMIODirectURL)) {
                            mergedMIO = mergeMIOs(mio, existingMIO);
                            isContained = true;
                            removalMIOURL = existingMIODirectURL;
                            break;

                        }
                        // else {
                        // mergedMIO = mergeMIOs(mio, existingMIO);
                        // isContained = true;
                        // break;
                        // }

                    }
                }
                if (!isContained) {
                    mioMap.put(mio.getDirectURL(), mio);
                } else {

                    if (mergedMIO != null) {
                        mioMap.put(mergedMIO.getDirectURL(), mergedMIO);
                        mioMap.remove(removalMIOURL);
                    }
                }

            }

        }

        for (Entry<String, MIO> mio : mioMap.entrySet()) {
            resultList.add(mio.getValue());
        }

        return resultList;
    }

    private MIO mergeMIOs(MIO masterMIO, MIO slaveMIO) {
        MIO returnValue = null;

        double featureCountExistingMio = 0;
        for (Entry<String, Double> feature : masterMIO.getFeatures().entrySet()) {
            featureCountExistingMio = +feature.getValue();
        }

        double featureCountNewMio = 0;
        for (Entry<String, Double> feature : slaveMIO.getFeatures().entrySet()) {
            featureCountNewMio = +feature.getValue();
        }
        // detect the longest directURL because this mostly works
        String masterMIOURL = masterMIO.getDirectURL();
        String slaveMIOURL = slaveMIO.getDirectURL();
        String mergedMIOURL = "";
        if (masterMIOURL.length() <= slaveMIOURL.length()) {
            mergedMIOURL = slaveMIOURL;

        } else {
            mergedMIOURL = masterMIOURL;
        }
     
        // prefer that MIO with the most 1-features
        if (featureCountExistingMio < featureCountNewMio) {
            slaveMIO.setDirectURL(mergedMIOURL);
            returnValue = slaveMIO;

        } else {
            masterMIO.setDirectURL(mergedMIOURL);
            returnValue = masterMIO;
        }
        return returnValue;
    }

    /**
     * Calculate trust.
     * 
     * @param retrievedMIOs the retrieved MIOs
     * @param mioPage the mio page
     * @param entity the entity
     * @return the list
     */
    private List<MIO> calcFeaturesAndInteractivity(final List<MIO> retrievedMIOs, final MIOPage mioPage,
            final Entity entity) {

        final boolean analyzeSWFContent = InCoFiConfiguration.getInstance().analyzeSWFContent;
        final MIOContextAnalyzer contextAnalyzer = new MIOContextAnalyzer(entity, mioPage);
        SWFContentAnalyzer swfContentAnalyzer = null;
        if (analyzeSWFContent) {
            swfContentAnalyzer = new SWFContentAnalyzer();
        }

        final MIOInteractivityAnalyzer interactivityAnalyzer = new MIOInteractivityAnalyzer();
//        final long timeStamp4 = System.currentTimeMillis();
        for (MIO mio : retrievedMIOs) {

            // first initialize all features
            mio.initializeFeatures();

            contextAnalyzer.setFeatures(mio);

            // analyze content of SWF-Files only
            if (analyzeSWFContent && mio.getMIOType().equalsIgnoreCase("flash")) {

                swfContentAnalyzer.analyzeContentAndSetFeatures(mio, entity);
            }
            // calculate Interactivity
            interactivityAnalyzer.setInteractivityGrade(mio, mioPage);
            // reset MIO-Infos for saving memory
            // mio.resetMIOInfos();
        }
//        System.out.println("Downloading and Feature- and Interactivity- Calculation finished in: " +DateHelper.getRuntime(timeStamp4));
        return retrievedMIOs;
    }

}
