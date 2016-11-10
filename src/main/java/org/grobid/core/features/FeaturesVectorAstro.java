package org.grobid.core.features;

import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.lexicon.AstroLexicon;

import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * Class for repesenting the features used for recognition of astronomical entity mentions.
 *
 * @author Patrice
 */
public class FeaturesVectorAstro {

	private AstroLexicon lexicon = AstroLexicon.getInstance();

    public String string = null;     // lexical feature
    public String label = null;     // label (the goal of the CRF labelling!) if known

    public String capitalisation = null;// one of INITCAP, ALLCAPS, NOCAPS
    public String digit;                // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;

    public String punctType = null;
    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    // OPENQUOTE, ENDQUOTE

	// lexical features to inject some vocabularies/dictionaries of astronomical names
    public boolean astroName = false; // can be part of an astronomical name without looking at the other 
									  // tokens around
	public boolean isAstroToken = false; // is part of an astronomical name

    public boolean commonName = false;

	public String shadowNumber = null; // Convert digits to “0” 
	
	public String wordShape = null; 
	// Convert upper-case letters to "X", lower- case letters to "x", digits to "d" and other to "c"  
	// there is also a trimmed variant where sequence of similar character shapes are reduced to one
	// converted character shape
	public String wordShapeTrimmed = null;

    public FeaturesVectorAstro() {
    }

    public String printVector() {
        if (string == null) return null;
        if (string.length() == 0) return null;
        StringBuffer res = new StringBuffer();

        // token string (1)
        res.append(string);

        // lowercase string
        res.append(" " + string.toLowerCase());

        // prefix (5)
        res.append(" " + TextUtilities.prefix(string, 1));
        res.append(" " + TextUtilities.prefix(string, 2));
        res.append(" " + TextUtilities.prefix(string, 3));
        res.append(" " + TextUtilities.prefix(string, 4));
        res.append(" " + TextUtilities.prefix(string, 5));

        // suffix (5)
        res.append(" " + TextUtilities.suffix(string, 1));
        res.append(" " + TextUtilities.suffix(string, 2));
        res.append(" " + TextUtilities.suffix(string, 3));
        res.append(" " + TextUtilities.suffix(string, 4));
        res.append(" " + TextUtilities.suffix(string, 5));

        // capitalisation (1)
        if (digit.equals("ALLDIGIT"))
            res.append(" NOCAPS");
        else
            res.append(" " + capitalisation);

        // digit information (1)
        res.append(" " + digit);

        // character information (1)
        if (singleChar)
            res.append(" 1");
        else
            res.append(" 0"); 

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

		// lexical information (1)
        if (astroName)
            res.append(" 1");
        else
            res.append(" 0");
		
		// lexical feature: belongs to a known astro name (1)
		if (isAstroToken)
			res.append(" 1");
        else
            res.append(" 0");

        // token length (1)
        //res.append(" " + string.length()); // /

		// shadow number (1)
		res.append(" " + shadowNumber); // /
		
		// word shape (1)
		res.append(" " + wordShape);
		
		// word shape trimmed (1)
		res.append(" " + wordShapeTrimmed);
		
        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "");
        else
            res.append(" 0");

        return res.toString();
    }

    /**
     * Add the features for the NER model.
     */
    static public FeaturesVectorAstro addFeaturesAstro(String line,
                                            String label,
                                            boolean isAstroToken,
                                            boolean isAstroPattern) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        FeaturesVectorAstro featuresVector = new FeaturesVectorAstro();
        StringTokenizer st = new StringTokenizer(line, "\t ");
        if (st.hasMoreTokens()) {
            String word = st.nextToken();

            if (st.hasMoreTokens())
                label = st.nextToken();

            featuresVector.string = word;
            featuresVector.label = label;

            if (word.length() == 1) {
                featuresVector.singleChar = true;
            }

            if (featureFactory.test_all_capital(word))
                featuresVector.capitalisation = "ALLCAPS";
            else if (featureFactory.test_first_capital(word))
                featuresVector.capitalisation = "INITCAP";
            else
                featuresVector.capitalisation = "NOCAPS";

            if (featureFactory.test_number(word))
                featuresVector.digit = "ALLDIGIT";
            else if (featureFactory.test_digit(word))
                featuresVector.digit = "CONTAINDIGIT";
            else
                featuresVector.digit = "NODIGIT";

            Matcher m0 = featureFactory.isPunct.matcher(word);
            if (m0.find()) {
                featuresVector.punctType = "PUNCT";
            }
            if ((word.equals("(")) | (word.equals("["))) {
                featuresVector.punctType = "OPENBRACKET";
            } else if ((word.equals(")")) | (word.equals("]"))) {
                featuresVector.punctType = "ENDBRACKET";
            } else if (word.equals(".")) {
                featuresVector.punctType = "DOT";
            } else if (word.equals(",")) {
                featuresVector.punctType = "COMMA";
            } else if (word.equals("-")) {
                featuresVector.punctType = "HYPHEN";
            } else if (word.equals("\"") | word.equals("\'") | word.equals("`")) {
                featuresVector.punctType = "QUOTE";
            }

            if (featuresVector.capitalisation == null)
                featuresVector.capitalisation = "NOCAPS";

            if (featuresVector.digit == null)
                featuresVector.digit = "NODIGIT";

            if (featuresVector.punctType == null)
                featuresVector.punctType = "NOPUNCT";

			if (featureFactory.test_common(word)) {
                featuresVector.commonName = true;
            }

            featuresVector.astroName = isAstroToken;

			featuresVector.isAstroToken = isAstroPattern; 

			featuresVector.shadowNumber = TextUtilities.shadowNumbers(word);
			
			featuresVector.wordShape = TextUtilities.wordShape(word);
			
			featuresVector.wordShapeTrimmed = TextUtilities.wordShapeTrimmed(word);
        }

        return featuresVector;
    }

}
	
	
