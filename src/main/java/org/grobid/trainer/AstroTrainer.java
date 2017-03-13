package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorAstro;
import org.grobid.core.lexicon.AstroLexicon;
import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.AstroProperties;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.Pair;
import org.grobid.trainer.evaluation.EvaluationUtilities;
import org.grobid.trainer.AstroAnnotationSaxHandler;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.EngineParsers;
import org.grobid.core.layout.PDFAnnotation;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.Block;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Training of the astronomical entity recognition model
 *
 * @author Patrice
 */
public class AstroTrainer extends SmectaAbstractTrainer {

    private AstroLexicon astroLexicon = null;

    public AstroTrainer() {
        this(0.00001, 20, 0);
    }
    
    public AstroTrainer(double epsilon, int window, int nbMaxIterations) {
    	super(GrobidModels.ASTRO);

		// adjusting CRF training parameters for this model
		this.epsilon = epsilon;
		this.window = window;
		this.nbMaxIterations = nbMaxIterations;

        astroLexicon = AstroLexicon.getInstance();
    }

    /**
     * Add the selected features to the model training for astro entities. For grobid-astro, we
	 * can have two types of training files: XML/TEI files where text content is annotated with
	 * astronimocal entities, and PDF files where the entities are annotated with an additional
	 * PDf layer. The two types of training files suppose two different process in order to 
	 * generate the CRF training file.    
     */
    public int createCRFPPData(File sourcePathLabel,
                               File outputPath) {
    	return createCRFPPData(sourcePathLabel, outputPath, null, 1.0);
    }

    /**
     * Add the selected features to the model training for astronomical entities. Split 
     * automatically all available labeled data into training and evaluation data 
     * according to a given split ratio.
     */
    public int createCRFPPData(final File corpusDir,
            final File trainingOutputPath,
            final File evalOutputPath,
            double splitRatio) {
    	return createCRFPPData(corpusDir, trainingOutputPath, evalOutputPath, splitRatio, true);
    }
    public int createCRFPPData(final File corpusDir,
                               final File trainingOutputPath,
                               final File evalOutputPath,
                               double splitRatio,
                               boolean splitRandom) {
        int totalExamples = 0;
        Writer writerTraining = null;
        Writer writerEvaluation = null;
        try {
            System.out.println("labeled corpus path: " + corpusDir.getPath());
            System.out.println("training data path: " + trainingOutputPath.getPath());
            System.out.println("evaluation data path: " + trainingOutputPath.getPath());

            // we convert first the tei files into the usual CRF label format
            // we process all tei files in the output directory
            File[] refFiles = corpusDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".tei") || name.toLowerCase().endsWith(".tei.xml");
                }
            });

            // the file for writing the training data
            writerTraining = new OutputStreamWriter(new FileOutputStream(trainingOutputPath), "UTF8");

            // the file for writing the evaluation data
            if (evalOutputPath != null)
				writerEvaluation = new OutputStreamWriter(new FileOutputStream(evalOutputPath), "UTF8");            

			// the active writer
			Writer writer = null;

            if (refFiles != null) {
	            System.out.println(refFiles.length + " TEI files");

	            // get a factory for SAX parser
	            SAXParserFactory spf = SAXParserFactory.newInstance();

	            String name;
	            for (int n = 0; n < refFiles.length; n++) {
	                File thefile = refFiles[n];
	                name = thefile.getName();
	                System.out.println(name);

	                AstroAnnotationSaxHandler handler = new AstroAnnotationSaxHandler();

	                //get a new instance of parser
	                SAXParser p = spf.newSAXParser();
	                p.parse(thefile, handler);

	                List<Pair<String, String>> labeled = handler.getLabeledResult();

	                // we need to add now the features to the labeled tokens
	                List<Pair<String, String>> bufferLabeled = null;
	                int pos = 0;

	                // segmentation into training/evaluation is done file by file
	                if (splitRandom) {
						if (Math.random() <= splitRatio)
							writer = writerTraining;
						else 
							writer = writerEvaluation;
	                }
	                else {
	                	if ((double)n/refFiles.length <= splitRatio)
							writer = writerTraining;
						else 
							writer = writerEvaluation;
	                }

	                // let's iterate by defined CRF input (separated by new line)
	                while (pos < labeled.size()) {
	                    bufferLabeled = new ArrayList<>();
	                    while (pos < labeled.size()) {
	                        if (labeled.get(pos).getA().equals("\n")) {
	                            pos++;
	                            break;
	                        }
	                        bufferLabeled.add(labeled.get(pos));
	                        pos++;
	                    }

	                    if (bufferLabeled.size() == 0)
	                        continue;

	                    List<OffsetPosition> astroTokenPositions = astroLexicon.inAstroNamesVectorLabeled(bufferLabeled);

	                    addFeatures(bufferLabeled, writer, astroTokenPositions);
	                    writer.write("\n");
	                }
	                writer.write("\n");
	            }
			}

			// we convert then the PDF files having entities annotated into the
			// CRf training format
			refFiles = corpusDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".pdf");
                }
            });

            if (refFiles != null) {
				EngineParsers parsers = new EngineParsers();
	            System.out.println(refFiles.length + " PDF files");

	            String name;
	            for (int n = 0; n < refFiles.length; n++) {
	                File thefile = refFiles[n];
	                name = thefile.getName();
	                System.out.println(name);

					// parse the PDF
					GrobidAnalysisConfig config = 
						new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().build();
					DocumentSource documentSource = 
						DocumentSource.fromPdf(thefile, config.getStartPage(), config.getEndPage());
					Document doc = parsers.getSegmentationParser().processing(documentSource, config);

					List<LayoutToken> tokenizations = doc.getTokenizations();

					// get the annotations
					List<PDFAnnotation> annotations = doc.getPDFAnnotations();

					// we can create the labeled data block per block
					int indexAnnotation = 0;
					List<Block> blocks = doc.getBlocks();

					// segmentation into training/evaluation is done file by file
					// it could be done block by block y moving the piece of code bellow
					// under the next loop on blocks bellow
					if (splitRandom) {
						if (Math.random() <= splitRatio)
							writer = writerTraining;
						else 
							writer = writerEvaluation;
	                }
	                else {
	                	if ((double)n/refFiles.length <= splitRatio)
							writer = writerTraining;
						else 
							writer = writerEvaluation;
	                }
					
					for(Block block : blocks) {
						List<Pair<String, String>> labeled = new ArrayList<Pair<String, String>>();
						String previousLabel = "";
						int startBlockToken = block.getStartToken();
						int endBlockToken = block.getEndToken();
						
						for(int p=startBlockToken; p < endBlockToken; p++) {
							LayoutToken token = tokenizations.get(p);
							//for(LayoutToken token : tokenizations) {
							if ( (token.getText() != null) &&
								 (token.getText().trim().length()>0) &&
								 (!token.getText().equals("\t")) && 
								 (!token.getText().equals("\n")) && 	  
								 (!token.getText().equals("\r")) ) {
								String theLabel = "<other>";
								for(int i=indexAnnotation; i<annotations.size(); i++) {
									PDFAnnotation currentAnnotation = annotations.get(i);
									// check if we are at least on the same page
									if (currentAnnotation.getPageNumber() < token.getPage())
										continue;
									else if (currentAnnotation.getPageNumber() > token.getPage())
										break;

									// check if we have an astro entity
									if ( (currentAnnotation.getType() == PDFAnnotation.Type.URI) && 
										(currentAnnotation.getDestination() != null) &&
										(currentAnnotation.getDestination().indexOf("simbad") != -1) ) {

										//System.out.println(currentAnnotation.toString() + "\n");

										if (currentAnnotation.cover(token)) {
											System.out.println(currentAnnotation.toString() + " covers " + token.toString());
											// the annotation covers the token position
											// we have an astro entity at this token position
											if (previousLabel.endsWith("<object>")) {
												theLabel = "<object>";
											}
											else {
												// we filter out entity starting with (
												if (!token.getText().equals("("))
													theLabel = "I-<object>";
											}
											break;
										}
									} 
								}
								Pair<String, String> thePair = 
									new Pair<String, String>(token.getText(), theLabel);

								// we filter out entity ending with a punctuation mark
								if (theLabel.equals("<other>") && previousLabel.equals("<object>")) {
									// check the previous token 
									Pair<String, String> theLastPair = labeled.get(labeled.size() - 1);
									String theLastToken = theLastPair.getA();
									if (theLastToken.equals(";") || 
										theLastToken.equals(".") || 
										theLastToken.equals(",") ) {
										theLastPair = new Pair(theLastToken, "<other>");
										labeled.set(labeled.size()-1, theLastPair);
									}
								}

								// add the current token
								labeled.add(thePair);
								previousLabel = theLabel;
						    }
						}
						// add features
	                    List<OffsetPosition> astroTokenPositions = astroLexicon.inAstroNamesVectorLabeled(labeled);

	                    addFeatures(labeled, writer, astroTokenPositions);
	                    writer.write("\n");
					}
					writer.write("\n");
				}
			}
        } catch (Exception e) {
            throw new GrobidException("An exception occured while training Grobid.", e);
        } finally {
        	try {
	        	if (writerTraining != null)
		        	writerTraining.close();
        	    if (writerEvaluation != null)
 	        	   writerEvaluation.close();
 	        } catch(IOException e) {
 	        	e.printStackTrace();
 	        }
        }
        return totalExamples;
    }

    @SuppressWarnings({"UnusedParameters"})
    private void addFeatures(List<Pair<String, String>> texts,
                             Writer writer,
                             List<OffsetPosition> astroTokenPositions) {
        int totalLine = texts.size();
        int posit = 0;
        int currentAstroIndex = 0;
        List<OffsetPosition> localPositions = astroTokenPositions;
        boolean isAstroPattern = false;
        try {
            for (Pair<String, String> lineP : texts) {
                String token = lineP.getA();
                if (token.trim().equals("@newline")) {
                    writer.write("\n");
                    writer.flush();
                }

                String label = lineP.getB();
                /*if (label != null) {
                    isAstroPattern = true;
                }*/

                // do we have an astro at position posit?
                if ((localPositions != null) && (localPositions.size() > 0)) {
                    for (int mm = currentAstroIndex; mm < localPositions.size(); mm++) {
                        if ((posit >= localPositions.get(mm).start) && 
						    (posit <= localPositions.get(mm).end)) {
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
                        FeaturesVectorAstro.addFeaturesAstro(token, label, 
							astroLexicon.inAstroDictionary(token), isAstroPattern);
                if (featuresVector.label == null)
                    continue;
                writer.write(featuresVector.printVector());
                writer.write("\n");
                writer.flush();
                posit++;
                isAstroPattern = false;
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
    }

    /**
     * Standard evaluation via the the usual Grobid evaluation framework.
     */
    public String evaluate() {
        File evalDataF = GrobidProperties.getInstance().getEvalCorpusPath(
                new File(new File("resources").getAbsolutePath()), model);

        File tmpEvalPath = getTempEvaluationDataPath();
        createCRFPPData(evalDataF, tmpEvalPath);

        return EvaluationUtilities.evaluateStandard(tmpEvalPath.getAbsolutePath(), getTagger());
    }
    
    public String splitTrainEvaluate(Double split, boolean random) {
    	System.out.println("PAths :\n"+getCorpusPath()+"\n"+GrobidProperties.getModelPath(model).getAbsolutePath()+"\n"+getTempTrainingDataPath().getAbsolutePath()+"\n"+getTempEvaluationDataPath().getAbsolutePath()+" \nrand "+random);
        
        File trainDataPath = getTempTrainingDataPath();
        File evalDataPath = getTempEvaluationDataPath();
        
        final File dataPath = trainDataPath;
        createCRFPPData(getCorpusPath(), dataPath, evalDataPath, split);
        GenericTrainer trainer = TrainerFactory.getTrainer();

        if (epsilon != 0.0) 
            trainer.setEpsilon(epsilon);
        if (window != 0)
            trainer.setWindow(window);
        if (nbMaxIterations != 0)
            trainer.setNbMaxIterations(nbMaxIterations);
        
        final File tempModelPath = new File(GrobidProperties.getModelPath(model).getAbsolutePath() + NEW_MODEL_EXT);
        final File oldModelPath = GrobidProperties.getModelPath(model);

        trainer.train(getTemplatePath(), dataPath, tempModelPath, GrobidProperties.getNBThreads(), model);

        // if we are here, that means that training succeeded
        renameModels(oldModelPath, tempModelPath);

        return EvaluationUtilities.evaluateStandard(evalDataPath.getAbsolutePath(), getTagger());
    }
    
    protected final File getCorpusPath() {
        return new File(AstroProperties.get("grobid.astro.corpusPath"));
    }
    
    protected final File getTemplatePath() {
        return new File(AstroProperties.get("grobid.astro.templatePath"));
    }

    /**
     * Command line execution.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        try {
            String pGrobidHome = "../grobid-home";
            String pGrobidProperties = "../grobid-home/config/grobid.properties";

            MockContext.setInitialContext(pGrobidHome, pGrobidProperties);
            GrobidProperties.getInstance();

            Trainer trainer = new AstroTrainer();
            AbstractTrainer.runTraining(trainer);
            AbstractTrainer.runEvaluation(trainer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                MockContext.destroyInitialContext();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}