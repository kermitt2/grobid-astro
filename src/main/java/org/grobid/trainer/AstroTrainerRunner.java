package org.grobid.trainer;

import java.io.File;

import javax.xml.bind.annotation.XmlElement;

import org.grobid.core.mock.MockContext;
import org.grobid.core.utilities.GrobidProperties;

/**
 * Training application for training a target model.
 * 
 * @author Patrice Lopez
 */
public class AstroTrainerRunner {

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
	protected static void initProcess(final String path2GbdHome, final String path2GbdProperties) {
		try {
			MockContext.setInitialContext(path2GbdHome, path2GbdProperties);
		} catch (final Exception exp) {
			System.err.println("Grobid initialisation failed: " + exp);
		}
		GrobidProperties.getInstance();
	}

	/**
	 * Command line execution.
	 * 
	 * @param args
	 *            Command line arguments.
	 */
	public static void main(String[] args) {
		if (args.length < 4) {
			throw new IllegalStateException(
					"Usage: {0 - train, 1 - evaluate, 2 - split, train and evaluate} {astro} -gH /path/to/Grobid/home -s { [0.0 - 1.0] - split ratio, optional} -b {epsilon, window, nbMax}");
		}

		RunType mode = RunType.getRunType(Integer.parseInt(args[0]));
		if ( (mode == RunType.SPLIT) && (args.length < 6) ) {
			throw new IllegalStateException(
					"Usage: {0 - train, 1 - evaluate, 2 - split, train and evaluate} {astro} -gH /path/to/Grobid/home -s { [0.0 - 1.0] - split ratio, optional} -b {epsilon, window, nbMax}");
		}

		String path2GbdHome = null;
		Double split = 0.0;
		
		boolean breakParams = false;
		double epsilon = 0.000001;
		int window = 20;
		int nbMaxIterations = 0;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-gH")) {
				if (i+1 == args.length) {
					throw new IllegalStateException("Missing path to Grobid home. ");
				}
				path2GbdHome = args[i + 1];
			}
			else if (args[i].equals("-s")) {
				if (i+1 == args.length) {
					throw new IllegalStateException("Missing split ratio value. ");
				}
				String splitRatio = args[i + 1];
				try {					
					split = Double.parseDouble(args[i + 1]);
				}
				catch(Exception e) {
					throw new IllegalStateException("Invalid split value: " + args[i + 1]);
				}
				
			}
			else if (args[i].equals("-b")) {
				if ((mode == RunType.TRAIN) && (args.length >= 8)) {
					breakParams = true;
					epsilon = Double.parseDouble(args[i+1]);
					window = Integer.parseInt(args[i+2]);
					nbMaxIterations = Integer.parseInt(args[i+3]);
				}
				else
					throw new IllegalStateException("Usage: {0 - train, 1 - evaluate, 2 - split, train and evaluate} {astro} -gH /path/to/Grobid/home -s { [0.0 - 1.0] - split ratio, optional} -b {epsilon, window, nbMax}");
			}
		}

		if (path2GbdHome == null) {
			throw new IllegalStateException(
					"Usage: {0 - train, 1 - evaluate, 2 - split, train and evaluate} {ner,nerfr,nersense} -gH /path/to/Grobid/home -s { [0.0 - 1.0] - split ratio, optional}");
		}

		final String path2GbdProperties = path2GbdHome + File.separator + "config" + File.separator + "grobid.properties";

		System.out.println("path2GbdHome=" + path2GbdHome + "   path2GbdProperties=" + path2GbdProperties);
		initProcess(path2GbdHome, path2GbdProperties);

		AstroTrainer trainer = new AstroTrainer();
		
		if (breakParams)
			trainer.setParams(epsilon, window, nbMaxIterations);

		switch (mode) {
		case TRAIN:
			AbstractTrainer.runTraining(trainer);
			break;
		case EVAL:
			AbstractTrainer.runEvaluation(trainer);
			break;
		case SPLIT:
			AbstractTrainer.runSplitTrainingEvaluation(trainer, split);
			break;
		default:
			throw new IllegalStateException("Invalid RunType: " + mode.name());
		}
	}
}