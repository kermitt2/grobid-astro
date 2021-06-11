package org.grobid.trainer;

import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.AstroConfiguration;
import org.grobid.core.utilities.GrobidProperties;

import java.util.Arrays;
import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Training application for training a target model.
 *
 * @author Patrice Lopez
 */
public class AstroTrainerRunner {

    private static final String USAGE = "Usage: {0 - train, 1 - evaluate, 2 - split, train and evaluate} {astro} "
            + "-s { [0.0 - 1.0] - split ratio, optional} "
            + "-b {epsilon, window, nbMax}"
            + "-t NBThreads";

    enum RunType {
        TRAIN, EVAL, SPLIT;

        public static RunType getRunType(int i) {
            for (RunType t : values()) {
                if (t.ordinal() == i) {
                    return t;
                }
            }

            throw new IllegalStateException("Unsupported RunType with ordinal " + i);
        }
    }

    /**
     * Initialize the batch.
     */
    protected static void initProcess(String grobidHome) {
        try {
            if (grobidHome == null)
                grobidHome = "../grobid-home/";
            
            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(grobidHome));
            grobidHomeFinder.findGrobidHomeOrFail();
            GrobidProperties.getInstance(grobidHomeFinder);
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed: " + exp);
        }
    }

    protected static void initProcess() {
        GrobidProperties.getInstance();
    }

    /**
     * Command line execution.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            throw new IllegalStateException(USAGE);
        }

        RunType mode = RunType.getRunType(Integer.parseInt(args[0]));
        if ((mode == RunType.SPLIT) && (args.length < 5)) {
            throw new IllegalStateException(USAGE);
        }

        AstroConfiguration astroConfiguration = null;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            astroConfiguration = mapper.readValue(new File("resources/config/grobid-astro.yaml"), AstroConfiguration.class);
        } catch(Exception e) {
            System.err.println("The config file does not appear valid, see resources/config/grobid-astro.yaml");
        }
        
        String path2GbdHome = astroConfiguration.getGrobidHome();
        System.out.println("path2GbdHome=" + path2GbdHome);
        initProcess(path2GbdHome);

        Double split = 0.0;

        boolean breakParams = false;
        double epsilon = 0.000001;
        int window = 20;
        int nbMaxIterations = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-t")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing Threads number. ");
                }
                int nbTreadsInt = 0;
                try {
                    nbTreadsInt = Integer.parseInt(args[i + 1]);
                } catch (Exception e) {
                    System.err.println("Warning: the thread number parameter is not a valid integer, " + args[i + 1] + " - using 0 as default thread number");
                    e.printStackTrace();
                }
                GrobidProperties.getInstance().setWapitiNbThreads(nbTreadsInt);
            } else if (args[i].equals("-s")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing split ratio value. ");
                }
                String splitRatio = args[i + 1];
                try {
                    split = Double.parseDouble(args[i + 1]);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid split value: " + args[i + 1]);
                }

            } else if (args[i].equals("-b")) {
                if ((mode == RunType.TRAIN) && (args.length >= 7)) {
                    breakParams = true;
                    epsilon = Double.parseDouble(args[i + 1]);
                    window = Integer.parseInt(args[i + 2]);
                    nbMaxIterations = Integer.parseInt(args[i + 3]);
                } else
                    throw new IllegalStateException(USAGE);
            }
        }

        if (path2GbdHome == null) {
            throw new IllegalStateException(
                    USAGE);
        }

        AstroTrainer trainer = new AstroTrainer();

        if (breakParams)
            trainer.setParams(epsilon, window, nbMaxIterations);

        switch (mode) {
            case TRAIN:
                AbstractTrainer.runTraining(trainer);
                break;
            case EVAL:
                System.out.println(AbstractTrainer.runEvaluation(trainer));
                break;
            case SPLIT:
                System.out.println(AbstractTrainer.runSplitTrainingEvaluation(trainer, split));
                break;
            default:
                throw new IllegalStateException("Invalid RunType: " + mode.name());
        }
    }
}
