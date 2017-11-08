package org.grobid.core.engines;

import org.apache.commons.io.IOUtils;
import org.grobid.core.data.AstroEntity;
import org.grobid.core.document.Document;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.AstroProperties;
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
public class AstroParserTest {
    private static Engine engine;

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            String pGrobidHome = AstroProperties.get("grobid.home");

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);
    
            System.out.println(">>>>>>>> GROBID_HOME="+GrobidProperties.get_GROBID_HOME_PATH());
        } catch (final Exception exp) {
            System.err.println("GROBID astro initialisation failed: " + exp);
            exp.printStackTrace();
        }

        engine = GrobidFactory.getInstance().createEngine();
    }

    @Before
    public void getTestResourcePath() {
        GrobidProperties.getInstance();
    }

    @Test
    public void testAstroParserText() throws Exception {
        System.out.println("testAstroParserText - testAstroParserText - testAstroParserText");
        String text = IOUtils.toString(this.getClass().getResourceAsStream("/text.txt"), StandardCharsets.UTF_8.toString());
        text = text.replaceAll("\\n", " ").replaceAll("\\t", " ");
        List<AstroEntity> entities = AstroParser.getInstance().processText(text);
        //System.out.println(text);
        //System.out.println(entities.size());
        assertThat(entities, hasSize(2));
    }

    //@Test
    public void testAstroParserPDF() throws Exception {
        Pair<List<AstroEntity>, Document> res = AstroParser.getInstance().processPDF(new File("./src/test/resources/annot.pdf"));
        List<AstroEntity> entities = res.getA();

        assertThat(entities, hasSize(19));
    }

}