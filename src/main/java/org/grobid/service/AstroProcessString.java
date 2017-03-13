package org.grobid.service;

import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.grobid.core.data.AstroEntity;
import org.grobid.core.engines.AstroParser;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author Patrice
 * 
 */
public class AstroProcessString {

	/**
	 * The class Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AstroProcessString.class);

	/**
	 * Parse a raw date and return the corresponding normalized date.
	 * 
	 * @param the
	 *            raw date string
	 * @return a response object containing the structured xml representation of
	 *         the date
	 */
	public static Response processText(String text) {
		LOGGER.debug(methodLogIn());
		Response response = null;
		StringBuilder retVal = new StringBuilder();
		AstroParser parser = AstroParser.getInstance();
		try {
			LOGGER.debug(">> set raw text for stateless service'...");
			
			List<AstroEntity> entities = null;
			text = text.replaceAll("\\n", " ").replaceAll("\\t", " ");
			long start = System.currentTimeMillis();
			entities = parser.processText(text);
			long end = System.currentTimeMillis();

			if (entities != null) {
				retVal.append("{ ");
				if (entities.size() == 0)
					retVal.append("\"entities\" : []");
				else {
					boolean first = true;
					for(AstroEntity entity : entities)	{
						if (first) {
							retVal.append("\"entities\" : [ ");
							first = false;
						} else {	
							retVal.append(", ");
						}
						retVal.append(entity.toJson());
					}
					retVal.append("]");
				}
				retVal.append(", \"runtime\" : " + (end-start));
				retVal.append("}");
			}
			String retValString = retVal.toString();
			if (!isResultOK(retValString)) {
				response = Response.status(Status.NO_CONTENT).build();
			} else {
				response = Response.status(Status.OK).entity(retValString).type(MediaType.TEXT_PLAIN).build();
			}
		} catch (NoSuchElementException nseExp) {
			LOGGER.error("Could not get an instance of AstroParser. Sending service unavailable.");
			response = Response.status(Status.SERVICE_UNAVAILABLE).build();
		} catch (Exception e) {
			LOGGER.error("An unexpected exception occurs. ", e);
			response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} 
		LOGGER.debug(methodLogOut());
		return response;
	}

	/**
	 * @return
	 */
	public static String methodLogIn() {
		return ">> " + AstroProcessString.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
	}

	/**
	 * @return
	 */
	public static String methodLogOut() {
		return "<< " + AstroProcessString.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
	}

	/**
	 * Check whether the result is null or empty.
	 */
	public static boolean isResultOK(String result) {
		return StringUtils.isBlank(result) ? false : true;
	}

}
