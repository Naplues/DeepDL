package zju.defect.locater;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import zju.defect.util.CSV_handler;
import zju.defect.util.FileUtil;

public class ThresholdLearning {

	private static CSV_handler myCSV = new CSV_handler();

	public static double LearnThreshold(String entropyValResultPath, boolean bugNgram) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> valEntropyResult = myCSV.getContentFromFile(new File(entropyValResultPath));
		List<Double> allEntropies = valEntropyResult.stream().map(i -> Double.parseDouble(i[9]))
				.collect(Collectors.toList());
		Set<Double> formalEntropy = new HashSet<Double>();

		List<String[]> tuningMidResult = new ArrayList<String[]>();
		String[] headers = new String[] { "Entropy", "tuningObj", "F1AsClassifier", "AveF1AllCommits", "AveF1HitBug",
				"HitBugCommits", "HitCleanCommits", "HitBugCommitsRatio", "HitCleanCommitsRatio",
				"AllCommits", "AllBugCommits", "AllCleanCommits"};

		Map<Double, Double> entropyF1Map = new HashMap<Double, Double>();
		DecimalFormat df = new DecimalFormat("#.00");

		for (int i = 0; i < allEntropies.size(); i++) {
			double thisEntropy = allEntropies.get(i);
			thisEntropy = Double.parseDouble(df.format(thisEntropy));
			if (!formalEntropy.contains(thisEntropy)) {
				formalEntropy.add(thisEntropy);

//				Map<String, Double> results = Evaluation.EvaluateF1HitCommitsAndAsClassifier(valEntropyResult,
//						thisEntropy, bugNgram);
				Map<String, Double> results = Evaluation.EvaluateF1HitCommitsAndAsClassifier(valEntropyResult,
						"bugNgram", "thisEntropy");

				double allCommits = results.get("allCommits");
				double allBugCommits = results.get("allBugCommits");
				double allCleanCommits = results.get("allCleanCommits");
				//double preClassifier = results.get("preClassifier");
				//double recallClassifier = results.get("preClassifier");
				double f1Classifier = results.get("f1Classifier");
				double hitBugAveF1 = results.get("hitBugAveF1");
				//double hitCleanAveF1 = results.get("hitCleanAveF1");
				double hitBugCommits = results.get("hitBugCommits");
				double hitCleanCommits = results.get("hitCleanCommits");
				double hitBugCommitsRaito = hitBugCommits / allCommits;
				double hitCleanCommitsRaito = hitCleanCommits / allCommits;
				double aveF1AllCommits = results.get("aveF1AllCommits");
				
				double aveRecallTrueBugs = results.get("aveRecallTrueBugs");
				double avePreTrueBugs = results.get("avePreTrueBugs");
				double aveMr2TrueBugs = results.get("aveMr2TrueBugs");
				double aveMapTrueBugs = results.get("aveMapTrueBugs");

				double tuningObj = Math.log(f1Classifier) + Math.log (aveF1AllCommits) + Math.log (hitBugCommitsRaito * hitBugAveF1)
					+ Math.log(hitCleanCommitsRaito);
				
				//double tuningObj = aveRecallTrueBugs + avePreTrueBugs + aveMr2TrueBugs + aveMapTrueBugs;

				String[] thisEntropyMidResult = new String[] { Double.toString(thisEntropy), Double.toString(tuningObj),
						Double.toString(f1Classifier), Double.toString(aveF1AllCommits), Double.toString(hitBugAveF1),
						Double.toString(hitBugCommits), Double.toString(hitCleanCommits),
						Double.toString(hitBugCommitsRaito), Double.toString(hitCleanCommitsRaito),
						Double.toString(allCommits), Double.toString(allBugCommits), Double.toString(allCleanCommits) };
				
				tuningMidResult.add(thisEntropyMidResult);

				entropyF1Map.put(thisEntropy, tuningObj);
			}
		}

		double maxEntropy = GetMaxKeyForValue(entropyF1Map);

		String tuningMidResultPath = "F://defectLocater//middleResult.csv";
		// String[] header = new String[] { "Entropy", "F1" };
		// FileUtil.printMap(entropyF1Map, mapPath, header);
		myCSV.writeToCsv(new File(tuningMidResultPath), headers, tuningMidResult);
		return maxEntropy;
	}

	private static double GetMaxKeyForValue(Map<Double, Double> entropyF1Map) {
		// TODO Auto-generated method stub
		double value = 0;
		double maxKey = 0;
		List<Double> list = new ArrayList<Double>();

		Iterator ite = entropyF1Map.entrySet().iterator();
		while (ite.hasNext()) {
			Map.Entry entry = (Map.Entry) ite.next();
			value = (double) entry.getValue();
			list.add(value);
			Collections.sort(list);

			if (value == list.get(list.size() - 1)) {
				maxKey = (double) entry.getKey();
			}
		}
		return maxKey;
	}

}
