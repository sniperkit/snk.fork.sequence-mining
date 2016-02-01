package sequencemining.eval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import sequencemining.main.InferenceAlgorithms.InferGreedy;
import sequencemining.main.SequenceMining;
import sequencemining.sequence.Sequence;
import sequencemining.transaction.Transaction;
import sequencemining.transaction.TransactionList;

public class ParityClassificationTask {

	private static final boolean VERBOSE = false;

	public static void main(final String[] args) throws IOException {

		final int N = 5;
		final int M = 3;
		final int L = 5;
		final int noInstances = 1_000;
		final String baseFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Classification/Parity/";
		final File dbFile = new File(baseFolder + "PARITY.txt");

		// Generate parity database
		generateParityProblem(N, M, L, noInstances, dbFile);
		final TransactionList dbTrans = SequenceMining.readTransactions(dbFile);

		// Mine SQS seqs
		final File tmpOut = File.createTempFile("classification_temp", ".txt");
		final Map<Sequence, Double> seqsSQS = StatisticalSequenceMining.mineSQSSequences(dbFile, tmpOut, 1);
		System.out.println(seqsSQS);
		// Generate SQS seq features
		// seqsSQS = removeSingletons(seqsSQS);
		final File featuresSQS = new File(baseFolder + "FeaturesSQS.txt");
		generateFeatures(dbTrans, seqsSQS.keySet(), featuresSQS, N, M);

		// Mine GOKRIMP seqs
		final File tmpOut2 = File.createTempFile("classification_temp", ".txt");
		final Map<Sequence, Double> seqsGOKRIMP = StatisticalSequenceMining.mineGoKrimpSequences(dbFile, tmpOut2);
		tmpOut2.delete();
		System.out.println(seqsGOKRIMP);
		// Generate GOKRIMP seq features
		// seqsGOKRIMP = removeSingletons(seqsGOKRIMP);
		final File featuresGOKRIMP = new File(baseFolder + "FeaturesGOKRIMP.txt");
		generateFeatures(dbTrans, seqsGOKRIMP.keySet(), featuresGOKRIMP, N, M);

		// Mine ISM seqs
		final int maxStructureSteps = 100_000;
		final int maxEMIterations = 1_000;
		final Map<Sequence, Double> seqsISM = SequenceMining.mineSequences(dbFile, new InferGreedy(), maxStructureSteps,
				maxEMIterations, null, false);
		System.out.println(seqsISM);
		// Generate ISM seq features
		// seqsISM = removeSingletons(seqsISM);
		final File featuresISM = new File(baseFolder + "FeaturesISM.txt");
		generateFeatures(dbTrans, seqsISM.keySet(), featuresISM, N, M);

		// Generate simple features
		final File featuresSimple = new File(baseFolder + "FeaturesSimple.txt");
		generateSimpleFeatures(dbTrans, featuresSimple, N, M);

		// Run MALLET Naive Bayes classifier
		MarkovClassificationTask.classify(baseFolder, featuresSQS);
		MarkovClassificationTask.classify(baseFolder, featuresGOKRIMP);
		MarkovClassificationTask.classify(baseFolder, featuresISM);
		MarkovClassificationTask.classify(baseFolder, featuresSimple);

	}

	private static void generateParityProblem(final int N, final int M, final int L, final int noInstances,
			final File outFile) throws IOException {

		// Set random number seed
		final Random random = new Random(1);

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate transaction database
		int count = 0;
		while (count < noInstances) {
			for (int i = 0; i < N; i++) {
				if (random.nextDouble() < 0.5) {
					for (int j = 0; j < M; j++) // shift by 2 for GoKRIMP
						out.print(2 + 2 * (i * M + j) + " -1 ");
				} else {
					for (int j = 0; j < M; j++) // shift by 2 for GoKRIMP
						out.print(2 + 2 * (i * M + j) + 1 + " -1 ");
				}
			}
			for (int k = M * N; k < M * N + L; k++) {
				int item;
				if (random.nextDouble() < 0.5)
					item = 2 * k;
				else
					item = 2 * k + 1;
				out.print(2 + item + " -1 "); // shift by 2 for GoKRIMP
			}
			out.print("-2\n");
			count++;
		}
		out.close();

		if (VERBOSE)
			printFileToScreen(outFile);

	}

	private static void generateFeatures(final TransactionList dbTrans, final Set<Sequence> seqs, final File outFile,
			final int N, final int M) throws IOException {

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate features
		for (final Transaction trans : dbTrans.getTransactionList()) {
			final int label = getLabel(trans, N, M);
			out.print(label + " ");
			int fNum = 0;
			for (final Sequence seq : seqs) {
				if (trans.contains(seq))
					out.print("f" + fNum + ":1 ");
				else
					out.print("f" + fNum + ":0 ");
				fNum++;
			}
			out.println();
		}
		out.close();

		if (VERBOSE)
			printFileToScreen(outFile);
	}

	private static void generateSimpleFeatures(final TransactionList dbTrans, final File outFile, final int N,
			final int M) throws IOException {

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate features
		for (final Transaction trans : dbTrans.getTransactionList()) {
			final int label = getLabel(trans, N, M);
			out.print(label + " ");
			int fNum = 0;
			for (final int item : trans.getItems()) {
				if (item % 2 == 0)
					out.print("f" + fNum + ":1 ");
				else
					out.print("f" + fNum + ":0 ");
				fNum++;
			}
			out.println();
		}
		out.close();

		if (VERBOSE)
			printFileToScreen(outFile);
	}

	private static int getLabel(final Transaction trans, final int N, final int M) {
		final List<Integer> items = trans.getItems();
		int score = 0;
		for (int i = 0; i < N; i++) {
			int parity = 0;
			for (int j = 0; j < M; j++)
				parity += items.get(i * M + j);
			if (parity % 2 == 0)
				score++;
		}
		return (score > N / 2.) ? 1 : 0;
	}

	public static void printFileToScreen(final File file) throws FileNotFoundException {
		final FileReader reader = new FileReader(file);
		final LineIterator it = new LineIterator(reader);
		while (it.hasNext()) {
			System.out.println(it.nextLine());
		}
		LineIterator.closeQuietly(it);
	}

	static <V> Map<Sequence, V> removeSingletons(final Map<Sequence, V> oldSeqs) {
		final Map<Sequence, V> newSeqs = new HashMap<>();
		for (final Entry<Sequence, V> entry : oldSeqs.entrySet()) {
			if (entry.getKey().size() > 1)
				newSeqs.put(entry.getKey(), entry.getValue());
		}
		return newSeqs;
	}

}