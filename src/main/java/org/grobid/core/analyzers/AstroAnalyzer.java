package org.grobid.core.analyzers;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lang.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Tokenizer for all Indo-European languages and identifying astronomical mentions.
 *
 * @author Patrice
 */

public class AstroAnalyzer implements org.grobid.core.analyzers.Analyzer {

    private static volatile AstroAnalyzer instance;

    public static AstroAnalyzer getInstance() {
        if (instance == null) {
            //double check idiom
            // synchronized (instanceController) {
                if (instance == null)
                    getNewInstance();
            // }
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        instance = new AstroAnalyzer();
    }

    /**
     * Hidden constructor
     */
    private AstroAnalyzer() {
    }

    public static final String DELIMITERS = " \n\r\t([^%‰°,:;?.!/)-–−=≈<>+\"“”‘’'`$]*\u2666\u2665\u2663\u2660\u00A0"
			+ "\u002D\u2010\u2011\u2012\u2013\u2014\u2015\u207B\u208B\u2212"
                        + "\u0096\u058A\u2043\uFE58\uFE63\uFF0D" // \u05BE ->8 \u1806 -> A 
                        + "\u002B"
                        + "\u002E\u2024\u2027\u2219\uFE52"
                        + "\u0027\u2032\uFF07"
                        + "\u003C"
                        + "\u003D"
                        + "\u003E"
                        + "\u0020\u00A0\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007"
                        + "\u2008\u2009\u200A\u202F\u205F\u3000\uF0A0";

    private static final String REGEX = "(?<=[a-zA-Z])(?=\\d)|(?<=\\d)(?=\\D)";

    public String getName() {
        return "AstroAnalyzer";
    }

    public List<String> tokenize(String text) {
        // TBD: if we want to support non Indo-European languages, we should make the tokenization
        // language specific
        return tokenize(text, null);
    }

    public List<String> tokenize(String text, Language lang) {
        List<String> result = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(text, DELIMITERS, true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            // in addition we split "letter" characters and digits
            String[] subtokens = token.split(REGEX);
            for (int i = 0; i < subtokens.length; i++) {
                result.add(subtokens[i]);
            }
        }
        return result;
    }

    public List<LayoutToken> tokenizeWithLayoutToken(String text) {
        List<LayoutToken> result = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(text, DELIMITERS, true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            // in addition we split "letter" characters and digits
            String[] subtokens = token.split(REGEX);
            for (int i = 0; i < subtokens.length; i++) {
                LayoutToken layoutToken = new LayoutToken();
                layoutToken.setText(subtokens[i]);
                result.add(layoutToken);
            }
        }

        return result;
    }

    public List<String> retokenize(List<String> chunks) {
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            result.addAll(tokenize(chunk));
        }
        return result;
    }

    public List<LayoutToken> retokenizeLayoutTokens(List<LayoutToken> tokens) {
        List<LayoutToken> result = new ArrayList<>();
        for (LayoutToken token : tokens) {
            result.addAll(tokenize(token));
        }
        return result;
    }

     public List<LayoutToken> tokenize(LayoutToken chunk) {
        List<LayoutToken> result = new ArrayList<>();
        String text = chunk.getText();
        StringTokenizer st = new StringTokenizer(text, DELIMITERS, true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            // in addition we split "letter" characters and digits
            String[] subtokens = token.split(REGEX);
            for (int i = 0; i < subtokens.length; i++) {
                LayoutToken theChunk = new LayoutToken(chunk); // deep copy
                theChunk.setText(subtokens[i]);
                result.add(theChunk);
            }
        }

        return result;
    } 
}
