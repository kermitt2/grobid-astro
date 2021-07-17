package org.grobid.core.engines;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import org.grobid.core.data.AstroEntity;
import org.grobid.core.document.Document;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.AstroConfiguration;
import org.grobid.core.main.GrobidHomeFinder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
            AstroConfiguration astroConfiguration = null;
            try {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                astroConfiguration = mapper.readValue(new File("resources/config/grobid-astro.yaml"), AstroConfiguration.class);
            } catch(Exception e) {
                System.out.println("The config file does not appear valid, see resources/config/grobid-astro.yaml");
                e.printStackTrace();
            }

            String pGrobidHome = astroConfiguration.getGrobidHome();

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);
    
            System.out.println(">>>>>>>> GROBID_HOME="+GrobidProperties.getInstance().getGrobidHome());
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
    @Ignore("It seems to fail over certain results")
    public void testAstroParserText() throws Exception {
        System.out.println("testAstroParserText - testAstroParserText - testAstroParserText");
        String text = IOUtils.toString(this.getClass().getResourceAsStream("/text.txt"), StandardCharsets.UTF_8.toString());
        text = text.replaceAll("\\n", " ").replaceAll("\\t", " ");
        List<AstroEntity> entities = AstroParser.getInstance().processText(text);
        //System.out.println(text);
        //System.out.println(entities.size());
        assertThat(entities, hasSize(5));
    }

    //@Test
    public void testAstroParserPDF() throws Exception {
        Pair<List<AstroEntity>, Document> res = AstroParser.getInstance().processPDF(new File("./src/test/resources/annot.pdf"));
        List<AstroEntity> entities = res.getLeft();

        assertThat(entities, hasSize(19));
    }

}