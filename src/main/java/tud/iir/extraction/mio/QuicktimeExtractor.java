/**
 * This is the Extractor for Quicktime (MOV) objects.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tud.iir.helper.HTMLHelper;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.MIO;

public class QuicktimeExtractor extends AbstractMIOTypeExtractor {

    /** The mioType. */
    private static transient String mioType = "quicktime";

    /** The mioPage. */
    private transient MIOPage mioPage = null;

    /** The entity. */
    private transient Entity entity = null;

    /** The regular expression for URL extraction. */
    private static transient String regExp = "(\".[^\"]*\\.mov\")|(\".[^\"]*\\.mov\\?.[^\"]*\")";

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#extractMIOsByType(tud.iir.extraction.mio.MIOPage,
     * tud.iir.knowledge.Entity)
     */
    @Override
    List<MIO> extractMIOsByType(final MIOPage mioPage, final Entity entity) {
        this.mioPage = mioPage;
        this.entity = entity;

        final List<MIO> mioList = new ArrayList<MIO>();
        final List<String> relevantTags = extractRelevantTags(mioPage.getContentAsString());

        mioList.addAll(analyzeRelevantTags(relevantTags));
        return mioList;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#extractRelevantTags(java.lang.String)
     */
    @Override
    final List<String> extractRelevantTags(final String mioPageContent) {
        String modMioPageContent = "";

        final List<String> relevantTags = new ArrayList<String>();

        // extract all <object>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(mioPageContent, "object"));

        // remove the object-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(mioPageContent, "object");

        // extract all remaining <embed>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(modMioPageContent, "embed"));

        // remove all <embed>-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(modMioPageContent, "embed");

        // extract all <script>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(modMioPageContent, "script"));
        return relevantTags;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#analyzeRelevantTags(java.util.List)
     */
    @Override
    final List<MIO> analyzeRelevantTags(final List<String> relevantTags) {

        final List<MIO> retrievedMIOs = new ArrayList<MIO>();
        final List<MIO> tempMIOs = new ArrayList<MIO>();

        for (String relevantTag : relevantTags) {
            tempMIOs.clear();
            tempMIOs.addAll(extractMioURL(relevantTag, mioPage, regExp, entity, mioType));

            // extract ALT-Text from object and embed-tags and add to MIO-Infos
            if (!relevantTag.toLowerCase(Locale.ENGLISH).startsWith("<script")) {

                for (MIO mio : tempMIOs) {
                    final String tempAltText = extractALTTextFromTag(relevantTag);
                    if (tempAltText.length() > 2) {
                        mio.setAltText(tempAltText);
                    }
                }
            }
            // extract surrounding Information(Headlines, TextContent) and add to MIO-infos
            for (MIO mio : tempMIOs) {
                extractSurroundingInfo(relevantTag, mioPage, mio);
            }
            retrievedMIOs.addAll(tempMIOs);
        }
        return retrievedMIOs;
    }
}
