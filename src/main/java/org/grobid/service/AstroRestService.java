package org.grobid.service;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.lexicon.AstroLexicon;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.AstroProperties;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;

/**
 * RESTful service for GROBID astro extension.
 *
 * @author Patrice
 */
@Singleton
@Path(AstroPaths.PATH_ASTRO)
public class AstroRestService implements AstroPaths {

    private static final Logger LOGGER = LoggerFactory.getLogger(AstroRestService.class);

    private static final String TEXT = "text";
    private static final String XML = "xml";
    private static final String PDF = "pdf";
    private static final String INPUT = "input";

    public AstroRestService() {
        LOGGER.info("Init Servlet AstroRestService.");
        LOGGER.info("Init lexicon and KB resources.");
        try {
            String pGrobidHome = AstroProperties.get("grobid.home");

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(pGrobidHome));
            GrobidProperties.getInstance(grobidHomeFinder);
    
            LOGGER.info(">>>>>>>> GROBID_HOME="+GrobidProperties.get_GROBID_HOME_PATH());

            LibraryLoader.load();
            AstroLexicon.getInstance();
        } catch (final Exception exp) {
            LOGGER.error("GROBID astro initialisation failed. ", exp);
        }

        LOGGER.info("Init of Servlet AstroRestService finished.");
    }

    @Path(PATH_ASTRO_TEXT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @POST
    public Response processText_post(@FormParam(TEXT) String text) {
        LOGGER.info(text);
        return AstroProcessString.processText(text);
    }

    @Path(PATH_ASTRO_TEXT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @GET
    public Response processText_get(@QueryParam(TEXT) String text) {
        LOGGER.info(text);
        return AstroProcessString.processText(text);
    }
	
	@Path(PATH_ANNOTATE_ASTRO_PDF)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("application/json")
	@POST
	public Response processPDFAnnotation(@FormDataParam(INPUT) InputStream inputStream) {
		return AstroProcessFile.processPDFAnnotation(inputStream);
	}
}
