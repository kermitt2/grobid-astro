package org.grobid.core.test;

import org.grobid.core.data.AstroEntity;
import org.grobid.core.engines.AstroParser;
import org.grobid.core.main.GrobidConstants;
import org.grobid.core.utilities.GrobidProperties;
import org.junit.Test;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.grobid.core.engines.Engine;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.mock.MockContext;

/**
 * @author Patrice 
 */
public class TestAstroParser {

    private String testPath = null;
    private String newTrainingPath = null;
    private static Engine engine;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockContext.setInitialContext();
        engine = GrobidFactory.getInstance().createEngine();
    }

    @AfterClass
    public static void destroyInitialContext() throws Exception {
        MockContext.destroyInitialContext();
    }

    private void getTestResourcePath() {
        testPath = GrobidConstants.TEST_RESOURCES_PATH;
        GrobidProperties.getInstance();
        newTrainingPath = GrobidProperties.getTempPath().getAbsolutePath();
    }

    @Test
    public void testAstroParserText() throws Exception {
        getTestResourcePath();

		String textPath = testPath + File.separator + "text.txt";
		String text = FileUtils.readFileToString(new File(textPath), "UTF-8");
		text = text.replaceAll("\\n", " ").replaceAll("\\t", " ");
		List<AstroEntity> entities = AstroParser.getInstance().processText(text);

		assertNotNull(entities);

		for(AstroEntity entity : entities) {
			System.out.println(entity.toString());
		}
    }

    @Test
    public void testAstroParserPDF() throws Exception {
        getTestResourcePath();

        String pdfPath = testPath + File.separator + "annot.pdf";
		
		List<AstroEntity> entities = AstroParser.getInstance().processPDF(new File(pdfPath));
		assertNotNull(entities);

		for(AstroEntity entity : entities) {
			System.out.println(entity.toString());
		}
    }

}