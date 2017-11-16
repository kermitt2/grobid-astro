package org.grobid.core.lexicon;

import org.grobid.core.analyzers.AstroAnalyzer;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.lexicon.FastMatcher;
import org.grobid.core.layout.LayoutToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Class for managing the lexical resources for astronomical entities.
 *
 * @author Patrice
 */
public class AstroLexicon {

    // NER base types
    public enum Astro_Type {
        UNKNOWN("UNKNOWN"),
        OBJECT("OBJECT");

        private String name;

        private Astro_Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static Logger LOGGER = LoggerFactory.getLogger(AstroLexicon.class);

    private Set<String> astroVocabulary = null;
    private FastMatcher astroPattern = null;

    private static volatile AstroLexicon instance;

    public static synchronized AstroLexicon getInstance() {
        if (instance == null)
            instance = new AstroLexicon();

        return instance;
    }

    private AstroLexicon() {
        // init the lexicon
        LOGGER.info("Init astro lexicon");
        astroVocabulary = new HashSet<String>();

        File file = new File(GrobidProperties.getGrobidHomePath()+"/../grobid-astro/resources/lexicon/astroVoc.txt");
        if (!file.exists()) {
            throw new GrobidResourceException("Cannot initialize astro dictionary, because file '" + 
                file.getAbsolutePath() + "' does not exists.");
        }
        if (!file.canRead()) {
            throw new GrobidResourceException("Cannot initialize astro dictionary, because cannot read file '" + 
                file.getAbsolutePath() + "'.");
        }

        BufferedReader dis = null;
        // read the lexicon file
        try {
            astroPattern = new FastMatcher(file, AstroAnalyzer.getInstance());

            dis = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            String l = null;
            while ((l = dis.readLine()) != null) {
                if (l.length() == 0) continue;
                List<String> tokens = AstroAnalyzer.getInstance().tokenize(l);
                for(String token : tokens) {
                    if (token.length() > 1) {
                        // should we filter out 100% numerical tokens?
                        if (!astroVocabulary.contains(token)) {
                            astroVocabulary.add(token);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new GrobidException("AstroLexicon file not found.", e);
        } catch (IOException e) {
            throw new GrobidException("Cannot read AstroLexicon file.", e);
        } finally {
            try {
                if (dis != null)
                    dis.close();
            } catch(Exception e) {
                throw new GrobidResourceException("Cannot close IO stream.", e);
            }
        }
    }
	
	public boolean inAstroDictionary(String string) {
		// here a lexical look-up...
		return astroVocabulary.contains(string);
	}

    public List<OffsetPosition> tokenPositionsAstroNamesVectorLabeled(List<Pair<String, String>> pairs) {
        List<OffsetPosition> results = astroPattern.matcherPairs(pairs);
        return results;
    }

    public List<OffsetPosition> tokenPositionsAstroNames(List<LayoutToken> vector) {
        List<OffsetPosition> results = astroPattern.matchLayoutToken(vector);
        return results;
    }
}
