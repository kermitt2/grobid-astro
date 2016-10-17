package org.grobid.core.lexicon;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.Pair;
import org.grobid.core.utilities.OffsetPosition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

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

    private static volatile AstroLexicon instance;

    public static synchronized AstroLexicon getInstance() {
        if (instance == null)
            instance = new AstroLexicon();

        return instance;
    }

    private AstroLexicon() {
    }
	
	public boolean inAstroDictionary(String string) {
		// here a lexical look-up...
		
		return false;
	}

    public List<OffsetPosition> inAstroNamesVectorLabeled(List<Pair<String, String>> pairs) {
        // ...
        
        return null;
    }

    public List<OffsetPosition> inAstroNamesVector(List<String> vector) {
        // ...
        
        return null;
    }
}
