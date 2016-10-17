package org.grobid.service;

import org.grobid.core.data.AstroEntity;
import org.grobid.core.document.Document;
import org.grobid.core.engines.Engine;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.AstroParser;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.IOUtilities;
import org.grobid.core.utilities.KeyGen;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Patrice
 */
public class AstroProcessFile {

    /**
     * The class Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AstroProcessFile.class);

    /**
     * Uploads the origin PDF, process it and return PDF annotations for references in JSON.
     *
     * @param inputStream the data of origin PDF
     * @return a response object containing the JSON annotations
     */
    public static Response processPDFAnnotation(final InputStream inputStream) {
        LOGGER.debug(methodLogIn()); 
        Response response = null;
        File originFile = null;
        AstroParser parser = AstroParser.getInstance();
        Engine engine = null;
        try {
            LibraryLoader.load();
            engine = GrobidFactory.getInstance().getEngine();
            originFile = IOUtilities.writeInputFile(inputStream);
            GrobidAnalysisConfig config = new GrobidAnalysisConfig.
                GrobidAnalysisConfigBuilder().build();

            String json = null;

            if (originFile == null) {
                response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
            } else {
                DocumentSource documentSource = DocumentSource.fromPdf(originFile);
                Document teiDoc = engine.fullTextToTEIDoc(originFile, config);
                // to be written!
                //json = AstroEntityVisualizer.getJsonAnnotations(teiDoc);

                IOUtilities.removeTempFile(originFile);

                if (json != null) {
                    response = Response
                            .ok()
                            .type("application/json")
                            .entity(json)
                            .build();
                }
                else {
                    response = Response.status(Status.NO_CONTENT).build();
                }
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an instance of AstroParser. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            IOUtilities.removeTempFile(originFile);
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    public static String methodLogIn() {
        return ">> " + AstroProcessFile.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public static String methodLogOut() {
        return "<< " + AstroProcessFile.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    /**
     * Check whether the result is null or empty.
     */
    public static boolean isResultOK(String result) {
        return StringUtils.isBlank(result) ? false : true;
    }

}
