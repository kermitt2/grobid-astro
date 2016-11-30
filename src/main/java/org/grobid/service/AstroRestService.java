package org.grobid.service;

import com.sun.jersey.spi.resource.Singleton;
import org.grobid.core.lexicon.AstroLexicon;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.*;

import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.spi.resource.Singleton;


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
    
    private static final String PDF_DIR_PATH = "/home/kaestle/it/djin2/api/uploads/";

    public AstroRestService() {
        LOGGER.info("Init Servlet AstroRestService.");
        LOGGER.info("Init lexicon and KB resources.");
        try {
            InitialContext intialContext = new javax.naming.InitialContext();
            String path2grobidHome = (String) intialContext.lookup("java:comp/env/org.grobid.home");
            String path2grobidProperty = (String) intialContext.lookup("java:comp/env/org.grobid.property");

            MockContext.setInitialContext(path2grobidHome, path2grobidProperty);

            System.out.println(path2grobidHome);
            System.out.println(path2grobidProperty);

            LibraryLoader.load();
            GrobidProperties.getInstance();
            AstroLexicon.getInstance();
        } catch (final Exception exp) {
            System.err.println("GROBID astro initialisation failed: " + exp);
            exp.printStackTrace();
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
	
	@Path(PATH_ANNOTATE_ASTRO_LOCAL_PDF)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED + ";charset=utf-8")
	@Produces("application/json")
	@POST
	public Response processLocalPDFAnnotation(@FormParam("filename") String filename) {
		File file = new File(PDF_DIR_PATH+filename);
		
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Local file not found. Please check your PDF_DIR_PATH.").build();
		}
		
		return AstroProcessFile.processPDFAnnotation(inputStream);
	}
}
