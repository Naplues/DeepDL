package zju.defect.locater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import zju.defect.util.CSV_handler;

public class EvaluationRandom {

	private static CSV_handler myCSV = new CSV_handler();

	public static Map<String, Double> EvaluateAndOutputEachCommitResultByRandom(String entropyTestResultCSVCleanNgram,
			String evaCSVEachTrueBugCommit) throws IOException {
		// TODO Auto-generated method stub

		List<String[]> resultEntropy = myCSV.getContentFromFile(new File(entropyTestResultCSVCleanNgram));
		Map<String, Double> evaluateByEntropy = EvaluateF1HitCommitsAndAsClassifierRandom(resultEntropy,
				evaCSVEachTrueBugCommit);

		return evaluateByEntropy;
	}

	private static Map<String, Double> EvaluateF1HitCommitsAndAsClassifierRandom(List<String[]> resultEntropy,
			String evaCSVEachTrueBugCommit) throws IOException {
		// TODO Auto-generated method stub
		String[] evaHeaders = new String[] { "commit_hash", "total_lines", "bug_lines", "mr2Cost", "mapCost", "top1",
				"top2", "top5", "top10", "aucec5", "aucec20", "aucec50"};
		List<String[]> evaResultForTP = new ArrayList<String[]>();
		List<String[]> evaResultForTN = new ArrayList<String[]>();
		List<String[]> evaResultForTrueBugs = new ArrayList<String[]>(); // bug_line > 0;
		Map<String, Double> typePerformance = new HashMap<String, Double>();

		// for whole classifier
		double tp = 0; // true positive
		double tn = 0; // true Negative
		double fp = 0; // false positive
		double fn = 0; // false negative

		List<String[]> tempOneCommit = new ArrayList<String[]>();

		String commit_hash = "";
		int lines = 0;
		int bugLines = 0;
		String[] tempCommit = new String[evaHeaders.length];

		double hitBug = 0;
		double hitClean = 0;
		double allBug = 0;
		double allClean = 0;
		double allCommits = 0;
		for (int i = 0; i < resultEntropy.size(); i++) {
			// for (int i=0; i<30; i++){
			String[] thisLine = resultEntropy.get(i);
			String tempCommitHash = thisLine[0];
			
			if (commit_hash.equals("")) {
				commit_hash = tempCommitHash;
			}

			if (tempCommitHash.equals(commit_hash) && i < resultEntropy.size() - 1) {
				tempOneCommit.add(thisLine);
				lines++;
				if (thisLine[7].toLowerCase().equals("true"))
					bugLines++;
			} else {
				if (i == resultEntropy.size() - 1) {
					tempOneCommit.add(thisLine);
					lines++;
					if (thisLine[7].toLowerCase().equals("true"))
						bugLines++;
				}
				allCommits++;
				// String commitLabel = thisLine[8];
				String classifyResult = Evaluation.GetClassifiyType(tempOneCommit, 0, "BugAddNgram");
				//Map<String, Double> costResults = ComputeCostByRandom(tempOneCommit, bugLines);
				Map<String, Double> costResults = ComputeCostByRandom2(tempOneCommit, bugLines);

				tempCommit[0] = commit_hash;
				tempCommit[1] = Integer.toString(lines);
				tempCommit[2] = Integer.toString(bugLines);
				tempCommit[3] = Double.toString(costResults.get("mr2"));
				tempCommit[4] = Double.toString(costResults.get("map"));
				tempCommit[5] = Double.toString(costResults.get("top1"));
				tempCommit[6] = Double.toString(costResults.get("top2"));
				tempCommit[7] = Double.toString(costResults.get("top5"));
				tempCommit[8] = Double.toString(costResults.get("top10"));
				tempCommit[9] = Double.toString(costResults.get("aucec5"));
				tempCommit[10] = Double.toString(costResults.get("aucec20"));
				tempCommit[11] = Double.toString(costResults.get("aucec50"));

				if (classifyResult.equals("tp")) {
					tp++;
					hitBug++;					
					//if (bugLines > 0 && bugLines <= (lines/2) && lines > 10) {
					if (bugLines > 0) {//  filter targets
						allBug++;
						evaResultForTP.add(tempCommit);
						evaResultForTrueBugs.add(tempCommit);
					} else {
						allClean++;
					}
				} else if (classifyResult.equals("tn")) {
					tn++;
					hitClean++;
					allClean++;
					evaResultForTN.add(tempCommit);
				} else if (classifyResult.equals("fp")) {
					fp++;
					allClean++;
				} else if (classifyResult.equals("fn")) {
					fn++;
					//if (bugLines > 0 && bugLines <= (lines/2) && lines > 10) {
					if (bugLines > 0) {//  filter targets
						evaResultForTrueBugs.add(tempCommit);
						allBug++;
					} else {
						allClean++;
					}
				} else {
					System.out.println("BugBugBugBug");
				}
				tempOneCommit = new ArrayList<String[]>();
				tempCommit = new String[evaHeaders.length];
				commit_hash = "";
				lines = 0;
				bugLines = 0;
				if (i < resultEntropy.size() - 1)
					i--;
			}
		}

		myCSV.writeToCsv(new File(evaCSVEachTrueBugCommit), evaHeaders, evaResultForTrueBugs);
		// System.out.println("allValCommits: " + allCommits);

		List<Double> allMr2TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[3]))
				.collect(Collectors.toList());
		List<Double> allMapTrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[4]))
				.collect(Collectors.toList());

		List<Double> allTop1TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[5]))
				.collect(Collectors.toList());
		List<Double> allTop2TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[6]))
				.collect(Collectors.toList());
		List<Double> allTop5TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[7]))
				.collect(Collectors.toList());
		List<Double> allTop10TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[8]))
				.collect(Collectors.toList());
		List<Double> allAucec5TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[9]))
				.collect(Collectors.toList());
		List<Double> allAucec20TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[10]))
				.collect(Collectors.toList());
		List<Double> allAucec50TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[11]))
				.collect(Collectors.toList());

		double aveMr2AllTrueBugs = allMr2TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
	    double aveMapAllTrueBugs = allMapTrueBug.stream().collect(Collectors.averagingDouble(i -> i));

		double aveAllTop1TrueBug = allTop1TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllTop2TrueBug = allTop2TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllTop5TrueBug = allTop5TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllTop10TrueBug = allTop10TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllAucec5TrueBug = allAucec5TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllAucec20TrueBug = allAucec20TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllAucec50TrueBug = allAucec50TrueBug.stream().collect(Collectors.averagingDouble(i -> i));

//		Collections.sort(allMr2TrueBug);
//		Collections.sort(allMapTrueBug);
//		Collections.sort(allTop1TrueBug);
//		Collections.sort(allTop2TrueBug);
//		Collections.sort(allTop5TrueBug);
//		
//		double aveMr2AllTrueBugs = allMr2TrueBug.get(allMr2TrueBug.size()/2);
//		double aveMapAllTrueBugs = allMapTrueBug.get(allMapTrueBug.size()/2);
//
//		double aveAllTop1TrueBug = allTop1TrueBug.get(allTop1TrueBug.size()/2);
//		double aveAllTop2TrueBug = allTop2TrueBug.get(allTop2TrueBug.size()/2);
//		double aveAllTop5TrueBug = allTop5TrueBug.get(allTop5TrueBug.size()/2);


		String allCommitsCount = "allCommits";
		String allBugCommitsCount = "allBugCommits";
		String allCleanCommitsCount = "allCleanCommits";
		String aveMr2TrueBugs = "aveMr2TrueBugs";
		String aveMapTrueBugs = "aveMapTrueBugs";
		String aveTop1Accuracy = "aveTop1Accuracy";
		String aveTop2Accuracy = "aveTop2Accuracy";
		String aveTop5Accuracy = "aveTop5Accuracy";
		String aveTop10Accuracy = "aveTop10Accuracy";
		String aveAucec5Accuracy = "aveAucec5Accuracy";
		String aveAucec20Accuracy = "aveAucec20Accuracy";
		String aveAucec50Accuracy = "aveAucec50Accuracy";


		typePerformance.put(allCommitsCount, allCommits);
		typePerformance.put(allBugCommitsCount, allBug);
		typePerformance.put(allCleanCommitsCount, allClean);
		typePerformance.put(aveMr2TrueBugs, aveMr2AllTrueBugs);
		typePerformance.put(aveMapTrueBugs, aveMapAllTrueBugs);
		typePerformance.put(aveTop1Accuracy, aveAllTop1TrueBug);
		typePerformance.put(aveTop2Accuracy, aveAllTop2TrueBug);
		typePerformance.put(aveTop5Accuracy, aveAllTop5TrueBug);
		typePerformance.put(aveTop10Accuracy, aveAllTop10TrueBug);
		typePerformance.put(aveAucec5Accuracy, aveAllAucec5TrueBug);
		typePerformance.put(aveAucec20Accuracy, aveAllAucec20TrueBug);
		typePerformance.put(aveAucec50Accuracy, aveAllAucec50TrueBug);

		return typePerformance;
	}

	private static Map<String, Double> ComputeCostByRandom(List<String[]> tempOneCommit, int bugLines) {
		// TODO Auto-generated method stub
		Map<String, Double> costResults = new HashMap<String, Double>();		

		double top1 = 0;
		double top2 = 0;
		double top5 = 0;
		double mr2 = 0;
		double map = 0;
		double top10 = 0;
		double aucec5 = 0; 
		double aucec20 = 0;
		double aucec50 = 0;
		
		// double threshold = 0.2;
		

		double randomTimes = 100;

		for (int r = 0; r < randomTimes; r++) {
			Collections.shuffle(tempOneCommit);
			int hit = 0;
			double hitInspected = 0;
			int[] bugRank = new int[bugLines];
			boolean topkState = false;
			
			Map<Integer, Double> linesCost = new HashMap<Integer, Double>();
			Map<Integer, Double> costArea = new HashMap<Integer, Double>(); // e.g.,k=20, means AUCEC_20
			
			for (int i = 0; i < tempOneCommit.size(); i++) {
				String[] thisLine = tempOneCommit.get(i);
				String commit_id = thisLine[0];
				//if (commit_id.equals("093b0e2370ebd4c133fa2f110a39ac53dd1bb699"))
				//	System.out.println("debug");
				String entropy = thisLine[9];
				String label = thisLine[7];
				if (label.toLowerCase().equals("true")) {
					bugRank[hit] = i + 1;
					hit++;
					hitInspected = hitInspected + 1;
					if (i == 0) {
						top1 += 1;
						top2 += 1;
						top5 += 1;
						top10 += 1;
						topkState = true;
					}
					else if (i == 1 && !topkState) {
						top2 += 1;
						top5 += 1;	
						top10 += 1;
						topkState = true;
					}
					else if (i > 1 && i < 5 && !topkState){
						top5 += 1;
						top10 += 1;
						topkState = true;
					}
					
					else if (i > 4 && i < 10 && !topkState){						
						top10 += 1;
						topkState = true;
					}
					else{
						// find after 10 lines
					}
				}
				
				linesCost.put(i+1, hitInspected/bugLines);
			}
			
			if (bugLines > 0) {
				  costArea = Evaluation.GenerateAucecByLinesCost(linesCost);
				  aucec5 += costArea.get(5);
				  aucec20 += costArea.get(20);
				  aucec50 += costArea.get(50);
				}
			
			if (bugRank.length > 0) {
				//mr2 += Evaluation.GetMRRByBugRank(bugRank, bugLines);
				double thisMr2 = 1.0 / bugRank[0];
				double thisMap = Evaluation.GetMapByBugRank(bugRank, bugLines);
				mr2 = mr2 + thisMr2;
				map = map + thisMap;
				//System.out.println("ThisMr2: " + thisMr2);
				//System.out.println("ThisMap: " + thisMap);
				//map += Evaluation.GetGMapByBugRank(bugRank, bugLines);
			}
		}   

		costResults.put("mr2", mr2/randomTimes);
		costResults.put("map", map/randomTimes);
		costResults.put("top1", top1/randomTimes);
		costResults.put("top2", top2/randomTimes);
		costResults.put("top5", top5/randomTimes);
		costResults.put("top10", top10/randomTimes);
		costResults.put("aucec5", aucec5/randomTimes);
		costResults.put("aucec20", aucec20/randomTimes);
		costResults.put("aucec50", aucec50/randomTimes);

		return costResults;
	}
	
	// fix random baseline by using median
	private static Map<String, Double> ComputeCostByRandom2 (List<String[]> tempOneCommit, int bugLines) {
		// TODO Auto-generated method stub
		Map<String, Double> costResults = new HashMap<String, Double>();		
		
		List<Double> top1List = new ArrayList<Double> ();
		List<Double> top2List = new ArrayList<Double> ();
		List<Double> top5List = new ArrayList<Double> ();
		List<Double> mr2List = new ArrayList<Double> ();
		List<Double> mapList = new ArrayList<Double> ();
		List<Double> top10List = new ArrayList<Double> ();
		List<Double> aucec5List = new ArrayList<Double> ();
		List<Double> aucec20List = new ArrayList<Double> ();
		List<Double> aucec50List = new ArrayList<Double> ();
		
		// double threshold = 0.2;
		

		int randomTimes = 101;

		for (int r = 0; r < randomTimes; r++) {
			double top1 = 0;
			double top2 = 0;
			double top5 = 0;
			double mr2 = 0;
			double map = 0;
			double top10 = 0;
			double aucec5 = 0; 
			double aucec20 = 0;
			double aucec50 = 0;
			
			Collections.shuffle(tempOneCommit);
			int hit = 0;
			double hitInspected = 0;
			int[] bugRank = new int[bugLines];
			boolean topkState = false;
			
			Map<Integer, Double> linesCost = new HashMap<Integer, Double>();
			Map<Integer, Double> costArea = new HashMap<Integer, Double>(); // e.g.,k=20, means AUCEC_20
			
			for (int i = 0; i < tempOneCommit.size(); i++) {
				String[] thisLine = tempOneCommit.get(i);
				String entropy = thisLine[10];
				String label = thisLine[7];
				if (label.toLowerCase().equals("true")) {
					bugRank[hit] = i + 1;
					hit++;
					hitInspected = hitInspected + 1;
					if (i == 0) {
						top1 += 1;
						top2 += 1;
						top5 += 1;
						top10 += 1;
						topkState = true;
					}
					else if (i == 1 && !topkState) {
						top2 += 1;
						top5 += 1;	
						top10 += 1;
						topkState = true;
					}
					else if (i > 1 && i < 5 && !topkState){
						top5 += 1;
						top10 += 1;
						topkState = true;
					}
					
					else if (i > 4 && i < 10 && !topkState){						
						top10 += 1;
						topkState = true;
					}
					else{
						// find after 10 lines
					}
				}
				
				linesCost.put(i+1, hitInspected/bugLines);
			}
			
			if (bugLines > 0) {
				  costArea = Evaluation.GenerateAucecByLinesCost(linesCost);
				  aucec5 += costArea.get(5);
				  aucec20 += costArea.get(20);
				  aucec50 += costArea.get(50);
				}
			
			if (bugRank.length > 0) {
				//mr2 += Evaluation.GetMRRByBugRank(bugRank, bugLines);
				mr2 = 1.0 / bugRank[0];
				map = Evaluation.GetMapByBugRank(bugRank, bugLines);
				//map += Evaluation.GetGMapByBugRank(bugRank, bugLines);
			}
			
			top1List.add(top1);
			top2List.add(top2);
			top5List.add(top5);
			top10List.add(top10);
			mr2List.add(mr2);
			mapList.add(map);
			aucec5List.add(aucec5);
			aucec20List.add(aucec20);
			aucec50List.add(aucec50);
		}
		
		int medianIndex = (randomTimes - 3)/2;

        double sortedMedianMr2 = (mr2List.stream().sorted((e1, e2) -> {return (e1.compareTo(e2));
		}).collect(Collectors.toList())).get(medianIndex);
        double sortedMedianMap = (mapList.stream().sorted((e1, e2) -> {return (e1.compareTo(e2));
   		}).collect(Collectors.toList())).get(medianIndex);
        double sortedMedianTop1 = (top1List.stream().sorted((e1, e2) -> {return (e1.compareTo(e2));
   		}).collect(Collectors.toList())).get(medianIndex);
        double sortedMedianTop2 = (top2List.stream().sorted((e1, e2) -> {return (e1.compareTo(e2));
   		}).collect(Collectors.toList())).get(medianIndex);
        double sortedMedianTop5 = (top5List.stream().sorted((e1, e2) -> {return (e1.compareTo(e2));
   		}).collect(Collectors.toList())).get(medianIndex);
        double sortedMedianTop10 = (top10List.stream().sorted((e1, e2) -> {return (e1.compareTo(e2));
   		}).collect(Collectors.toList())).get(medianIndex);
        double sortedMedianAucec5 = (aucec5List.stream().sorted((e1, e2) -> {return (e1.compareTo(e2));
   		}).collect(Collectors.toList())).get(medianIndex);
        double sortedMedianAucec20 = (aucec20List.stream().sorted((e1, e2) -> {return (e1.compareTo(e2));
   		}).collect(Collectors.toList())).get(medianIndex);
        double sortedMedianAucec50 = (aucec50List.stream().sorted((e1, e2) -> {return (e1.compareTo(e2));
   		}).collect(Collectors.toList())).get(medianIndex);

		costResults.put("mr2", sortedMedianMr2);
		costResults.put("map", sortedMedianMap);
		costResults.put("top1", sortedMedianTop1);
		costResults.put("top2", sortedMedianTop2);
		costResults.put("top5", sortedMedianTop5);
		costResults.put("top10", sortedMedianTop10);
		costResults.put("aucec5", sortedMedianAucec5);
		costResults.put("aucec20", sortedMedianAucec20);
		costResults.put("aucec50", sortedMedianAucec50);

		return costResults;
	}
}
