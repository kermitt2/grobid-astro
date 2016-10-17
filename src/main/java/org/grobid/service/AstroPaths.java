package org.grobid.service;

/**
 * This interface only contains the path extensions for accessing the astronomical entity recognition service.
 *
 * @author Patrice
 *
 */
public interface AstroPaths {
    /**
     * path extension for astro service.
     */
    public static final String PATH_ASTRO = "/";
    
    /**
     * path extension for extracting astronomical entities from a text.
     */
    public static final String PATH_ASTRO_TEXT= "processAstroText";

    /**
     * path extension for extracting astronomical entities from an TEI file 
	 * (for instance produced by GROBID or Pub2TEI).
     */
    public static final String PATH_ASTRO_XML= "processAstroTEI";

    /**
     * path extension for extracting astonomical entities from a PDF file.
     */
    public static final String PATH_ASTRO_PDF= "processAstroPDF";

    /**
     * path extension for annotating a PDF file with the recognized astronomical entities.
     */
    public static final String PATH_ANNOTATE_ASTRO_PDF= "annotateAstroPDF";
}
