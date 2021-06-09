package org.grobid.core.lexicon;

import org.apache.commons.io.IOUtils;
import org.grobid.core.analyzers.AstroAnalyzer;
import org.grobid.core.data.AstroEntity;
import org.grobid.core.document.Document;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.AstroProperties;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;

/**
 * @author Patrice
 */
public class AstroLexiconTest {
    private static AstroLexicon astroLexicon;

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            String pGrobidHome = AstroProperties.get("grobid.home");

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);
    
            System.out.println(">>>>>>>> GROBID_HOME="+GrobidProperties.getInstance().getGrobidHome());
        } catch (final Exception exp) {
            System.err.println("GROBID astro initialisation failed: " + exp);
            exp.printStackTrace();
        }

        astroLexicon = AstroLexicon.getInstance();
    }

    //@Test
    public void testTokenPositionsAstroNames() throws Exception {
        String testString = "GRB 10002 and other GRBs, but also GRB 050219. Still we have Magellanic Clouds around and M4-37934 in the corner, of M 4 or other NGC.";

        List<LayoutToken> tokens = AstroAnalyzer.getInstance().tokenizeWithLayoutToken(testString);
        List<OffsetPosition> astroTokenPositions = astroLexicon.tokenPositionsAstroNames(tokens);

        /*for(OffsetPosition position : astroTokenPositions) {
            for(int i=position.start; i <= position.end; i++)
                System.out.print(tokens.get(i));
            System.out.println(" / " + position.start + " " + position.end);
        }*/
        assertThat(astroTokenPositions, hasSize(8));
    }

    //@Test
    public void testTokenPositionsAstroNameShort() throws Exception {
        String testString = "GRBs";

        List<LayoutToken> tokens = AstroAnalyzer.getInstance().tokenizeWithLayoutToken(testString);
        List<OffsetPosition> astroTokenPositions = astroLexicon.tokenPositionsAstroNames(tokens);

        /*for(OffsetPosition position : astroTokenPositions) {
            for(int i=position.start; i <= position.end; i++)
                System.out.print(tokens.get(i));
            System.out.println(" / " + position.start + " " + position.end);
        }*/
        assertThat(astroTokenPositions, hasSize(1));

        testString = "GRBs.";

        tokens = AstroAnalyzer.getInstance().tokenizeWithLayoutToken(testString);
        astroTokenPositions = astroLexicon.tokenPositionsAstroNames(tokens);

        /*for(OffsetPosition position : astroTokenPositions) {
            for(int i=position.start; i <= position.end; i++)
                System.out.print(tokens.get(i));
            System.out.println(" / " + position.start + " " + position.end);
        }*/
        assertThat(astroTokenPositions, hasSize(1));
    }

    //@Test
    public void testTokenPositionsAstroNameComplex() throws Exception {
        String testString = "there is M4-37934 in the corner";

        List<LayoutToken> tokens = AstroAnalyzer.getInstance().tokenizeWithLayoutToken(testString);
        List<OffsetPosition> astroTokenPositions = astroLexicon.tokenPositionsAstroNames(tokens);

        /*for(OffsetPosition position : astroTokenPositions) {
            for(int i=position.start; i <= position.end; i++) 
                System.out.print(tokens.get(i));
            System.out.println(" / " + position.start + " " + position.end);
        }*/

        assertThat(astroTokenPositions, hasSize(2));
    }

}