package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorAstro;
import org.grobid.core.lexicon.AstroLexicon;
import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.Pair;
import org.grobid.trainer.evaluation.EvaluationUtilities;
import org.grobid.trainer.AstroAnnotationSaxHandler;
	
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
public class AstroTrainer extends AbstractTrainer {

    private AstroLexicon astroLexicon = null;

    public AstroTrainer() {
        super(GrobidModels.ASTRO);
		// adjusting CRF training parameters for this model
		epsilon = 0.000001;
		window = 20;

        astroLexicon = AstroLexicon.getInstance();
    }

    /**
     * Add the selected features to the model training for astronomical entities
     */
    public int createCRFPPData(final File corpusDir,
                               final File trainingOutputPath,
                               final File evalOutputPath,
                               double splitRatio) {
        // TBD, see training for other models
        return 0;
    }

    /**
     * Add the selected features to the model training for astro entities
     */
    public int createCRFPPData(File sourcePathLabel,
                               File outputPath) {
        int totalExamples = 0;
        try {
            System.out.println("sourcePathLabel: " + sourcePathLabel);
            System.out.println("outputPath: " + outputPath);

            // we convert the tei files into the usual CRF label format
            // we process all tei files in the output directory
            File[] refFiles = sourcePathLabel.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".tei") || name.toLowerCase().endsWith(".tei.xml");
                }
            });

            if (refFiles == null) {
                return 0;
            }

            System.out.println(refFiles.length + " files");

            // the file for writing the training data
            Writer writer = new OutputStreamWriter(new FileOutputStream(outputPath), "UTF8");

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

            writer.close();
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
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
                if (label != null) {
                    isAstroPattern = true;
                }

                // do we have an astro at position posit?
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
                        FeaturesVectorAstro.addFeaturesAstro(token, label, astroLexicon.inAstroDictionary(token), isAstroPattern);
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