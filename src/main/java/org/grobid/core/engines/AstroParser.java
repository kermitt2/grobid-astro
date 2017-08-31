package org.grobid.core.engines;

import nu.xom.Attribute;
import nu.xom.Element;
import org.apache.commons.io.FileUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.AstroAnalyzer;
import org.grobid.core.data.AstroEntity;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentPiece;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.AstroTaggingLabels;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.features.FeaturesVectorAstro;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.lexicon.AstroLexicon;
import org.grobid.core.sax.TextChunkSaxHandler;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.*;
import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;

/**
 * Astronomical mentions extraction.
 *
 * @author Patrice
 */
public class AstroParser extends AbstractParser {
    private static final Logger logger = LoggerFactory.getLogger(AstroParser.class);

    private static volatile AstroParser instance;

    public static AstroParser getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    /**
     * Create a new instance.
     */
    private static synchronized void getNewInstance() {
        instance = new AstroParser();
    }

    private AstroLexicon astroLexicon = null;
	private EngineParsers parsers;

    private AstroParser() {
        super(GrobidModels.ASTRO);
        astroLexicon = AstroLexicon.getInstance();
		parsers = new EngineParsers();
    }

    /**
     * Extract all Astro Objects from a simple piece of text.
     */
    public List<AstroEntity> processText(String text) throws Exception {
        if (isBlank(text)) {
            return null;
        }
        List<AstroEntity> entities = new ArrayList<AstroEntity>();
        try {
            text = text.replace("\n", " ");
            text = text.replace("\t", " ");
            List<LayoutToken> tokens = AstroAnalyzer.getInstance().tokenizeWithLayoutToken(text);

            if (tokens.size() == 0) {
                return null;
            }

            String ress = null;
            List<String> texts = new ArrayList<>();
            for (LayoutToken token : tokens) {
                if (!token.getText().equals(" ") && !token.getText().equals("\t") && !token.getText().equals("\u00A0")) {
                    texts.add(token.getText());
                }
            }

            // to store astronomical name positions (names coming from the optional dictionary)
            List<OffsetPosition> astroTokenPositions = astroLexicon.inAstroNamesVector(texts);
            ress = addFeatures(texts, astroTokenPositions);
            String res;
            try {
                res = label(ress);
            } catch (Exception e) {
                throw new GrobidException("CRF labeling for astro parsing failed.", e);
            }
//System.out.println(res);
            entities = extractAstroEntities(text, res, tokens);
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }

        return entities;
    }

	/**
	  * Extract all Astro Objects from a pdf file, with 
	  */
    public Pair<List<AstroEntity>,Document> processPDF(File file) throws IOException {

        List<AstroEntity> entities = new ArrayList<AstroEntity>();
        Document doc = null;
        try {
			GrobidAnalysisConfig config = 
				new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().build();
			DocumentSource documentSource = 
				DocumentSource.fromPdf(file, config.getStartPage(), config.getEndPage());
			doc = parsers.getSegmentationParser().processing(documentSource, config);

            // here we process the relevant textual content of the document

            // for refining the process based on structures, we need to filter
            // segment of interest (e.g. header, body, annex) and possibly apply 
            // the corresponding model to further filter by structure types 

            // from the header, we are interested in title, abstract and keywords
            SortedSet<DocumentPiece> documentParts = doc.getDocumentPart(SegmentationLabels.HEADER);
            if (documentParts != null) {
                String header = parsers.getHeaderParser().getSectionHeaderFeatured(doc, documentParts, true);
                List<LayoutToken> tokenizationHeader = doc.getTokenizationParts(documentParts, doc.getTokenizations());
                String labeledResult = null;
                if ((header != null) && (header.trim().length() > 0)) {
                    labeledResult = parsers.getHeaderParser().label(header);

                    BiblioItem resHeader = new BiblioItem();
                    //parsers.getHeaderParser().processingHeaderSection(false, doc, resHeader);
                    resHeader.generalResultMapping(doc, labeledResult, tokenizationHeader);

                    // title
                    List<LayoutToken> titleTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_TITLE);
                    if (titleTokens != null) {
                        processLayoutTokenSequence(titleTokens, entities);
                    } 

                    // abstract
                    List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                    if (abstractTokens != null) {
                        processLayoutTokenSequence(abstractTokens, entities);
                    } 

                    // keywords
                    List<LayoutToken> keywordTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_KEYWORD);
                    if (keywordTokens != null) {
                        processLayoutTokenSequence(keywordTokens, entities);
                    }
                }
            }

            // process selected structures in the body,
            documentParts = doc.getDocumentPart(SegmentationLabels.BODY);
            if (documentParts != null) {
                // full text processing
                Pair<String, LayoutTokenization> featSeg = parsers.getFullTextParser().getBodyTextFeatured(doc, documentParts);
                if (featSeg != null) {
                    // if featSeg is null, it usually means that no body segment is found in the
                    // document segmentation
                    String bodytext = featSeg.getA();

                    LayoutTokenization tokenizationBody = featSeg.getB();
                    String rese = null;
                    if ( (bodytext != null) && (bodytext.trim().length() > 0) ) {               
                        rese = parsers.getFullTextParser().label(bodytext);
                    } else {
                        logger.debug("Fulltext model: The input to the CRF processing is empty");
                    }

                    // do not process the references markers, figure markers, table markers, 
                    // formula markers, formula label and the formula
                    List<TaggingLabel> toProcess = Arrays.asList(TaggingLabels.PARAGRAPH, TaggingLabels.ITEM, 
                        TaggingLabels.SECTION, TaggingLabels.FIGURE, TaggingLabels.TABLE);
                    List<LayoutTokenization> documentBodyTokens = 
                        FullTextParser.getDocumentFullTextTokens(toProcess, rese, tokenizationBody.getTokenization());

                    if (documentBodyTokens != null) {
                            processLayoutTokenSequences(documentBodyTokens, entities);
                    }
                }
            }

            // we don't process references (although reference titles could be relevant)
            // acknowledgement? 

            // we can process annexes
            documentParts = doc.getDocumentPart(SegmentationLabels.ANNEX);
            if (documentParts != null) {
                processDocumentPart(documentParts, doc, entities);
            }

            // footnotes are also relevant?
            /*documentParts = doc.getDocumentPart(SegmentationLabel.FOOTNOTE);
            if (documentParts != null) {
                processDocumentPart(documentParts, doc, entities);
            }*/

        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("Cannot process pdf file: " + file.getPath());
        }

        Collections.sort(entities);
        return new Pair<List<AstroEntity>,Document>(entities, doc);
    }

    /**
     * Process with the astro model a segment coming from the segmentation model
     */
    private List<AstroEntity> processDocumentPart(SortedSet<DocumentPiece> documentParts, 
                                                  Document doc,
                                                  List<AstroEntity> entities) {
        List<LayoutToken> tokenizationParts = doc.getTokenizationParts(documentParts, doc.getTokenizations());
        return processLayoutTokenSequence(tokenizationParts, entities);
    }

    /**
     * Process with the astro model an arbitrary sequence of LayoutToken objects
     */ 
    private List<AstroEntity> processLayoutTokenSequence(List<LayoutToken> layoutTokens, 
                                                            List<AstroEntity> entities) {
        List<LayoutTokenization> layoutTokenizations = new ArrayList<LayoutTokenization>();
        layoutTokenizations.add(new LayoutTokenization(layoutTokens));
        return processLayoutTokenSequences(layoutTokenizations, entities);
    }

    /**
     * Process with the astro model a set of arbitrary sequence of LayoutTokenization
     */ 
    private List<AstroEntity> processLayoutTokenSequences(List<LayoutTokenization> layoutTokenizations, 
                                                  List<AstroEntity> entities) {
        for(LayoutTokenization layoutTokenization : layoutTokenizations) {
            List<LayoutToken> layoutTokens = layoutTokenization.getTokenization();

            // List<LayoutToken> for the selected segment
            List<LayoutToken> tokenizationParts = layoutTokens;

            // text of the selected segment
            String text = LayoutTokensUtil.toText(layoutTokens);
            
            // list of textual tokens of the selected segment
            List<String> texts = getTexts(tokenizationParts);
            
            // positions for lexical match
            List<OffsetPosition> astroTokenPositions = astroLexicon.inAstroNamesVector(texts);
            
            // string representation of the feature matrix for CRF lib
            String ress = addFeatures(texts, astroTokenPositions);     
            
            // labeled result from CRF lib
            String res = label(ress);

            entities.addAll(extractAstroEntities(text, res, tokenizationParts));
        }
        return entities;
    }

	/**
	 *
	 */
    public int batchProcess(String inputDirectory,
                            String outputDirectory,
                            boolean isRecursive) throws IOException {
		// TBD
        return 0;
    }

    /**
     * Give the list of textual tokens from a list of LayoutToken
     */
    private static List<String> getTexts(List<LayoutToken> tokenizations) {
        List<String> texts = new ArrayList<>();
        for (LayoutToken token : tokenizations) {
            if (isNotEmpty(trim(token.getText())) && 
                !token.getText().equals(" ") &&
                !token.getText().equals("\n") && 
                !token.getText().equals("\r") &&  
                !token.getText().equals("\t") && 
                !token.getText().equals("\u00A0")) {
                    texts.add(token.getText());
            }
        }
        return texts;
    }

    /**
     * Process the content of the specified input file and format the result as training data.
     * <p>
     * Input file can be (i)) PDF (.pdf) and it is assumed that we have a scientific article which will
     * be processed by GROBID full text first, (ii) some text (.txt extension).
	 *
	 * Note that we could consider a third input type which would be a TEI file resuling from the
	 * conversion of a publisher's native XML file following Pub2TEI transformatiom/standardization.
     *
     * @param inputFile input file
     * @param pathTEI   path to TEI with annotated training data
     * @param id        id
     */
    public void createTraining(String inputFile,
                               String pathTEI,
                               int id) throws Exception {
        File file = new File(inputFile);
        if (!file.exists()) {
            throw new GrobidException("Cannot create training data because input file can not be accessed: " + inputFile);
        }

        Element root = getTEIHeader(id);
        if (inputFile.endsWith(".txt") || inputFile.endsWith(".TXT")) {
            root = createTrainingText(file, root);
        } else if (inputFile.endsWith(".pdf") || inputFile.endsWith(".PDF")) {
            root = createTrainingPDF(file, root);
        }

        if (root != null) {
            //System.out.println(XmlBuilderUtils.toXml(root));
            try {
                FileUtils.writeStringToFile(new File(pathTEI), XmlBuilderUtils.toXml(root));
            } catch (IOException e) {
                throw new GrobidException("Cannot create training data because output file can not be accessed: " + pathTEI);
            }
        }
    }

	/**
	 * Generate training data with the current model using new files located in a given directory.
	 * the generated training data can then be corrected manually to be used for updating the
	 * astro CRF model.
     */
    @SuppressWarnings({"UnusedParameters"})
    public int createTrainingBatch(String inputDirectory,
                                   String outputDirectory,
                                   int ind) throws IOException {
        try {
            File path = new File(inputDirectory);
            if (!path.exists()) {
                throw new GrobidException("Cannot create training data because input directory can not be accessed: " + inputDirectory);
            }

            File pathOut = new File(outputDirectory);
            if (!pathOut.exists()) {
                throw new GrobidException("Cannot create training data because ouput directory can not be accessed: " + outputDirectory);
            }

            // we process all pdf files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".pdf") || name.endsWith(".PDF") ||
                            name.endsWith(".txt") || name.endsWith(".TXT");// ||
//                            name.endsWith(".xml") || name.endsWith(".tei") ||
 //                           name.endsWith(".XML") || name.endsWith(".TEI");
                }
            });

            if (refFiles == null)
                return 0;

            System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
            if (ind == -1) {
                // for undefined identifier (value at -1), we initialize it to 0
                n = 1;
            }
            for (final File file : refFiles) {
                try {
                    String pathTEI = outputDirectory + "/" + file.getName().substring(0, file.getName().length() - 4) + ".training.tei.xml";
                    createTraining(file.getAbsolutePath(), pathTEI, n);
                } catch (final Exception exp) {
                    logger.error("An error occured while processing the following pdf: "
                            + file.getPath() + ": " + exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }

	/**
	  * Generate training data from a text file
	  */
    private Element createTrainingText(File file, Element root) throws IOException {
        String text = FileUtils.readFileToString(file, "UTF-8");

        Element textNode = teiElement("text");
        // for the moment we suppose we have english only...
        textNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        // we process the text paragraph by paragraph
        String lines[] = text.split("\n");
        StringBuilder paragraph = new StringBuilder();
        List<AstroEntity> entities = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() != 0) {
                paragraph.append(line).append("\n");
            }
            if (((line.length() == 0) || (i == lines.length - 1)) && (paragraph.length() > 0)) {
                // we have a new paragraph
                text = paragraph.toString().replace("\n", " ").replace("\r", " ").replace("\t", " ");
                List<LayoutToken> tokens = AstroAnalyzer.getInstance().tokenizeWithLayoutToken(text);

                if (tokens.size() == 0)
                    continue;

                String ress = null;
                List<String> texts = new ArrayList<>();
                for (LayoutToken token : tokens) {
                    if (!token.getText().equals(" ") && !token.getText().equals("\t") && !token.getText().equals("\u00A0")) {
                        texts.add(token.getText());
                    }
                }

                // to store unit term positions
                List<OffsetPosition> astroTokenPositions = astroLexicon.inAstroNamesVector(texts);
                ress = addFeatures(texts, astroTokenPositions);
                String res = null;
                try {
                    res = label(ress);
                } catch (Exception e) {
                    throw new GrobidException("CRF labeling for quantity parsing failed.", e);
                }
                entities = extractAstroEntities(text, res, tokens);

                textNode.appendChild(trainingExtraction(entities, text, tokens));
                paragraph = new StringBuilder();
            }
        }
        root.appendChild(textNode);

        return root;
    }

	/**
	  * Generate training data from a PDf file
	  */
    private Element createTrainingPDF(File file, Element root) throws IOException {
        // first we apply GROBID fulltext model on the PDF to get the full text TEI
        Document teiDoc = null;
        try {
            teiDoc = GrobidFactory.getInstance().createEngine().fullTextToTEIDoc(file, GrobidAnalysisConfig.defaultInstance());
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("Cannot create training data because GROBIL full text model failed on the PDF: " + file.getPath());
        }
        if (teiDoc == null) {
            return null;
        }

        String teiXML = teiDoc.getTei();
		FileUtils.writeStringToFile(new File(file.getPath()+".tei.xml"), teiXML);

        // we parse this TEI string similarly as for createTrainingXML

        List<AstroEntity> entities = null;

        Element textNode = teiElement("text");
        // for the moment we suppose we have english only...
        textNode.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));

        try {
            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            TextChunkSaxHandler handler = new TextChunkSaxHandler();

            //get a new instance of parser
            SAXParser p = spf.newSAXParser();
            p.parse(new InputSource(new StringReader(teiXML)), handler);

            List<String> chunks = handler.getChunks();
            for (String text : chunks) {
                text = text.toString().replace("\n", " ").replace("\r", " ").replace("\t", " ");
                // the last one is a special "large" space missed by the regex "\\p{Space}+" used on the SAX parser
                if (text.trim().length() == 0)
                    continue;
                List<LayoutToken> tokenizations = AstroAnalyzer.getInstance().tokenizeWithLayoutToken(text);

                if (tokenizations.size() == 0)
                    continue;

                String ress = null;
                List<String> texts = new ArrayList<String>();
                for (LayoutToken token : tokenizations) {
                    if (!token.getText().equals(" ") && 
                        !token.getText().equals("\t") && 
                        !token.getText().equals("\u00A0")) {
                        texts.add(token.getText());
                    }
                }

                // to store unit term positions
                List<OffsetPosition> astroTokenPositions = astroLexicon.inAstroNamesVector(texts);
                ress = addFeatures(texts, astroTokenPositions);
                String res = null;
                try {
                    res = label(ress);
                } catch (Exception e) {
                    throw new GrobidException("CRF labeling for astro parsing failed.", e);
                }
                entities = extractAstroEntities(text, res, tokenizations);

                textNode.appendChild(trainingExtraction(entities, text, tokenizations));
            }
            root.appendChild(textNode);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("Cannot create training data because input PDF/XML file can not be parsed: " + file.getPath());
        }

        return root;
    }

    @SuppressWarnings({"UnusedParameters"})
    public String addFeatures(List<String> texts,
                               List<OffsetPosition> astroTokenPositions) {
        int totalLine = texts.size();
        int posit = 0;
        int currentAstroIndex = 0;
        List<OffsetPosition> localPositions = astroTokenPositions;
        boolean isAstroPattern = false;
        StringBuilder result = new StringBuilder();
        try {
            for (String token : texts) {
                if (token.trim().equals("@newline")) {
                    result.append("\n");
                    continue;
                }

                isAstroPattern = true;

                // do we have a unit at position posit?
                if ((localPositions != null) && (localPositions.size() > 0)) {
                    for (int mm = currentAstroIndex; mm < localPositions.size(); mm++) {
                        if ((posit >= localPositions.get(mm).start) && (posit <= localPositions.get(mm).end)) {
                            isAstroPattern = true;
                            currentAstroIndex = mm;
                            break;
                        } else if (posit < localPositions.get(mm).start) {
                            isAstroPattern = false;
                            break;
                        } else if (posit > localPositions.get(mm).end) {
                            continue;
                        }
                    }
                }

                FeaturesVectorAstro featuresVector =
                        FeaturesVectorAstro.addFeaturesAstro(token, null,
                                astroLexicon.inAstroDictionary(token), isAstroPattern);
                result.append(featuresVector.printVector());
                result.append("\n");
                posit++;
                isAstroPattern = false;
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return result.toString();
    }

    /**
     * Extract identified astronomical entities from a CRF labelled text.
     */
    public List<AstroEntity> extractAstroEntities(String text,
                                                	String result,
                                                	List<LayoutToken> tokenizations) {
        List<AstroEntity> entities = new ArrayList<>();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.ASTRO, result, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        AstroEntity currentEntity = null;
        AstroLexicon.Astro_Type openEntity = null;

        int pos = 0; // position in term of characters for creating the offsets

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> theTokens = cluster.concatTokens();
            String clusterContent = LayoutTokensUtil.toText(cluster.concatTokens()).trim();

            if ( (pos < text.length()-1) && (text.charAt(pos) == ' ') )
                pos += 1;
            int endPos = pos;
            boolean start = true;
            for (LayoutToken token : theTokens) {
                if (token.getText() != null) {
                    if (start && token.getText().equals(" ")) {
                        pos++;
                        endPos++;
                        continue;
                    }
                    if (start)
                        start = false;
                    endPos += token.getText().length();
                }
            }

            if ( (endPos > 0) && (text.charAt(endPos-1) == ' ') )
                endPos--;

            if (clusterLabel.equals(AstroTaggingLabels.OBJECT)) {
            	if (currentEntity == null) {
                    currentEntity = new AstroEntity();
                }

                currentEntity.setRawForm(clusterContent);
                currentEntity.setOffsetStart(pos);
                currentEntity.setOffsetEnd(endPos);
                currentEntity.setType(AstroLexicon.Astro_Type.OBJECT);

				List<BoundingBox> boundingBoxes = BoundingBoxCalculator.calculate(cluster.concatTokens());
				currentEntity.setBoundingBoxes(boundingBoxes);

				entities.add(currentEntity);
				currentEntity = null;
            }
            
            pos = endPos;
        }

        return entities;
    }

	/**
	 *  Add XML annotations corresponding to entities in a piece of text, to be included in
	 *  generated training data.
	 */
    public Element trainingExtraction(List<AstroEntity> entities, String text, List<LayoutToken> tokenizations) {
        Element p = teiElement("p");

        int pos = 0;
		if ( (entities == null) || (entities.size() == 0) )
			p.appendChild(text);
        for (AstroEntity entity : entities) {
            Element entityElement = teiElement("rs");

            if (entity.getType() == AstroLexicon.Astro_Type.OBJECT) {
                entityElement.addAttribute(new Attribute("type", "astro-object"));

                int startE = entity.getOffsetStart();
                int endE = entity.getOffsetEnd();

				p.appendChild(text.substring(pos, startE));
                entityElement.appendChild(text.substring(startE, endE));
            }
            p.appendChild(entityElement);
        }
        p.appendChild(text.substring(pos, text.length()));

        return p;
    }

	/**
	 *  Create a standard TEI header to be included in the TEI training files.
	 */
    private Element getTEIHeader(int id) {
        Element tei = teiElement("tei");
        Element teiHeader = teiElement("teiHeader");

        if (id != -1) {
            Element fileDesc = teiElement("fileDesc");
            fileDesc.addAttribute(new Attribute("xml:id", "http://www.w3.org/XML/1998/namespace", "_" + id));
            teiHeader.appendChild(fileDesc);
        }

        Element encodingDesc = teiElement("encodingDesc");

        Element appInfo = teiElement("appInfo");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String dateISOString = df.format(new java.util.Date());

        Element application = teiElement("application");
        application.addAttribute(new Attribute("version", GrobidProperties.getVersion()));
        application.addAttribute(new Attribute("ident", "GROBID"));
        application.addAttribute(new Attribute("when", dateISOString));

        Element ref = teiElement("ref");
        ref.addAttribute(new Attribute("target", "https://github.com/kermitt2/grobid"));
        ref.appendChild("A machine learning software for extracting information from scholarly documents");

        application.appendChild(ref);
        appInfo.appendChild(application);
        encodingDesc.appendChild(appInfo);
        teiHeader.appendChild(encodingDesc);
        tei.appendChild(teiHeader);

        return tei;
    }

	/**
	 *  Create training data from PDF with annotation layers corresponding to the entities.
	 */
	public int boostrapTrainingPDF(String inputDirectory,
                                   String outputDirectory,
                                   int ind) {
		return 0;
	}
}
