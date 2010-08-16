package tud.iir.extraction.mio;

import java.util.List;
import java.util.Locale;

public class MIOInteractivityAnalyzer {

    /** The weak interaction indicators. */
    final transient private List<String> weakInteractionIndicators;
    
    /** The strong interaction indicators. */
    final transient private List<String> strongInteractionIndicators;

    /**
     * Instantiates a new mIO interactivity analyzer.
     */
    public MIOInteractivityAnalyzer() {
        this.weakInteractionIndicators = InCoFiConfiguration.getInstance().getWeakInteractionIndicators();
        this.strongInteractionIndicators = InCoFiConfiguration.getInstance().getStrongInteractionIndicators();
    }

    /**
     * Sets the interactivity grade.
     * If textual content exists, mostly the MIO is strong.
     * If the fileSize is bigger than 2097152 Byte (=2MB), mostly the MIO is a video and thats why weak.
     * 
     * @param mio the mio
     * @param mioPage the mioPage
     */
    public void setInteractivityGrade(final MIO mio, final MIOPage mioPage) {

//        if (mio.getTextContentLength() > 2) {
//            mio.setInteractivityGrade("strong");
//        } else {
            if (mio.getFileSize() > 2097152) {
                mio.setInteractivityGrade("weak");
            } else {
                mio.setInteractivityGrade(calcInteractivityGrade(mio, mioPage));
            }
//        }

    }

    /**
     * Calculates interactivity-grade from fileName, pageTitle, headline and surroundingText.
     * 
     * @param mio the mio
     * @param mioPage the mioPage
     * @return the string
     */
    private String calcInteractivityGrade(final MIO mio, final MIOPage mioPage) {
        String returnValue = "unclear";

        final double fileNameiGrade = calcSingleValue(mio.getFileName());
        final double pageTitleiGrade = calcSingleValue(mioPage.getTitle());
        double headlineiGrade = 0.;
        if (mio.getInfos().containsKey("previousHeadlines")) {
            final String headline = (String) mio.getInfos().get("previousHeadlines").get(0);
            if (headline.length() > 1) {
                headlineiGrade = calcSingleValue(headline);
            }

        }
        double surroundingTextiGrade = (double) 0;
        if (mio.getInfos().containsKey("surroundingText")) {
            final String surroundingText = (String) mio.getInfos().get("surroundingText").get(0);
            if (surroundingText.length() > 1) {
                surroundingTextiGrade = calcSingleValue(surroundingText);
            }
        }
        final double result = fileNameiGrade + pageTitleiGrade + headlineiGrade + surroundingTextiGrade;
        if (result > 0) {
            returnValue = "strong";
        }
        if (result < 0) {
            returnValue = "weak";
        }

        return returnValue;
    }

    /**
     * Calculates single value existing weakIndicators are dominating over strongIndicators.
     * 0 means unclear, 1 is strong, -1 is weak
     * 
     * @param checkString the check string
     * @return the double
     */
    private double calcSingleValue(final String checkString) {
        final String modCheckString = checkString.toLowerCase(Locale.ENGLISH);
        double returnValue = 0;

        final int weakIndicators = getNumberOfInteractivityIndicators(modCheckString, false);
        final int strongIndicators = getNumberOfInteractivityIndicators(modCheckString, true);

        if (weakIndicators > 0) {

            returnValue = -1;

        } else {
            if (strongIndicators > 0) {
                returnValue = 1;
            }
        }
        return returnValue;
    }

    /**
     * Contains interactivity indicator.
     * 
     * @param checkString the check string
     * @param checkStrong the check strong
     * @return true, if successful
     */
    private int getNumberOfInteractivityIndicators(final String checkString, final boolean checkStrong) {
        int returnValue = 0;
        List<String> interactionIndicators = weakInteractionIndicators;
        // String[] isWeakIndicator = { "unboxing", "video", "preview", "review", "movie", "trailer", "promotion",
        // "youtube", "player", "logo" };
        if (checkStrong) {
            // final String[] isStrongIndicator = { "interactive", "click", "try", "360", "view", "index", "main",
            // "spin",
            // "tour", "virtual", "gallery", "play", "drag", "keys", "game", "microsite" };
            // isWeakIndicator = isStrongIndicator;
            interactionIndicators = strongInteractionIndicators;
        }

        for (String indicator : interactionIndicators) {
            if (checkString.contains(indicator)) {
                returnValue++;
            }
        }
        return returnValue;

    }
    //
    // /**
    // * The main method.
    // *
    // * @param args the arguments
    // */
    // public static void main(String[] args) {
    // // MIOInteractivityAnalyzer iAn = new MIOInteractivityAnalyzer();
    // // System.out
    // // .println(iAn
    // //
    // .calcSingleValue("http://g-ecx.images-amazon.com/images/G/01/am3/20100615163706920/AMPlayer._V191513928_.swf"));
    // }

}
