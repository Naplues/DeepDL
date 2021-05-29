package zju.defect.locater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import slp.core.example.JavaRunner;
import zju.defect.util.FileUtil;

public class LocalModel {

	public static void Run(String modelType, String ngramType, int ngramLength) throws IOException {

		// String finalAllProjectsResults = DefectLocater.root +
		// "allProjectsResultsLocal.csv";
		Map<String, Map<String, String>> projectTestEntropy = new HashMap<String, Map<String, String>>();
		
		for (String project : (DefectLocater.allProjectLocalPaths.keySet())) {
			Map<String, String> testTrainPathes = DefectLocater.allProjectLocalPaths.get(project);
			Map<String, String> testEntropyPathes = new HashMap<String, String>();
           // Set<String> writedPath = new HashSet<String>();
            int countLines = 0; 
			for (String testPath : testTrainPathes.keySet()) {
				String testSetPathJava = testPath;
				String trainSetPathJava = testTrainPathes.get(testPath);					
				
				String root = testPath.substring(0, testPath.lastIndexOf("_test.java"));
				String testSetPathCSV = root + "_test.csv";
				
				String trainSetPathCSV = root + "_train.csv";
				String entropyTrainResultCSV = root + "_entropyTrainResult.csv";				
				String entropyTestResultCSV = root + "_entropyTestResult.csv";
                
				String valSetPathJava = "";
				String valSetPathCSV = "";
				String entropyValResultCSV = "";
				
				List<String[]> testSetForCount = DefectLocater.myCSV.getContentFromFile(new File(testSetPathCSV));
				countLines = countLines + testSetForCount.size();
				
				//if (!writedPath.contains(entropyTestResultCSV)){
				//	writedPath.add(entropyTestResultCSV);
				//}
				//else{
				//	System.out.println(entropyTestResultCSV);
				//}

				JavaRunner jr = new JavaRunner();					
//				jr.Initilation(trainSetPathJava,trainSetPathCSV, testSetPathJava, testSetPathCSV, valSetPathJava, valSetPathCSV,
//						entropyTrainResultCSV, entropyTestResultCSV, entropyValResultCSV, ngramLength);

				jr.Initilation(trainSetPathJava,trainSetPathCSV, testSetPathJava, testSetPathCSV, entropyTestResultCSV, ngramLength);

				jr.LocateModeling(modelType, ngramType);				
				
				//NormalizeLocalEntropy(entropyTestResultCSV); // normalize the entropy value for a file or a sub-directory
				
				testEntropyPathes.put(testSetPathJava, entropyTestResultCSV);
			}
			
			System.out.println(project + " total lines:" + countLines);

			projectTestEntropy.put(project, testEntropyPathes);
		}
		
		CombineAllTestEntropyResultsByProject(projectTestEntropy, modelType, ngramType);
	}

	private static void NormalizeLocalEntropy(String entropyTestResultCSV) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> entropyTestResult = DefectLocater.myCSV.getContentFromFile(new File(entropyTestResultCSV));
		List<String[]> norEntropyTestResult = new ArrayList<String[]>();
		String[] header = DefectLocater.myCSV.getHeaderFromFile(new File(entropyTestResultCSV));
		
		List<Double> entropyValues = entropyTestResult.stream().map(i->Double.parseDouble(i[9])).collect(Collectors.toList());
		OptionalDouble min = entropyValues.stream().mapToDouble(i->i).min();
		OptionalDouble max = entropyValues.stream().mapToDouble(i->i).max();
		
		for (int i=0; i<entropyTestResult.size(); i++){
             String[] thisLine = entropyTestResult.get(i);
             double thisEntropy = Double.parseDouble(thisLine[9]);
             double norEntropy = (thisEntropy - min.getAsDouble()) / (max.getAsDouble() - min.getAsDouble());
             thisLine[9] = Double.toString(norEntropy);
             norEntropyTestResult.add(thisLine);            
		}
		
		DefectLocater.myCSV.writeToCsv(new File(entropyTestResultCSV), header, norEntropyTestResult);	
	}

	private static void CombineAllTestEntropyResultsByProject(Map<String, Map<String, String>> projectTestEntropy,
			String modelType, String ngramType) throws IOException {
		// TODO Auto-generated method stub
		//List<String[]> modelResults = new ArrayList<String[]>();
		//String finalAllProjectsResultsLocal = DefectLocater.root + "allProjectsResultsLocal.csv";
		//String[] headers = new String[] { "Project", "aveTop1Accuracy", "aveTop2Accuracy", "aveTop5Accuracy",
		//		"aveMr2TrueBugs", "aveMapTrueBugs", "allCommits", "allBugCommits", "allCleanCommits" };

		for (String project : projectTestEntropy.keySet()) {
			// String thisProject = DefectLocater.projects[i];
			// String inputGuruReport = root + thisProject + "//" + thisProject
			// + ".csv";
			List<String[]> entropyResultLocal = new ArrayList<String[]>();
			String[] header = { "" };
			String entropyResultLocalPath = DefectLocater.root + project + "//" + modelType
					+ "//" + project +  "_entropyTestResult"+modelType+ngramType+".csv";

			Map<String, String> testEntropyResult = projectTestEntropy.get(project);
			for (String testPath : testEntropyResult.keySet()) {
				String entropyResultPath = testEntropyResult.get(testPath);
				List<String[]> thisEntropyResult = DefectLocater.myCSV.getContentFromFile(new File(entropyResultPath));
				entropyResultLocal = FileUtil.CombineList(entropyResultLocal, thisEntropyResult);
				header = DefectLocater.myCSV.getHeaderFromFile(new File(entropyResultPath));
			}
			
			List<String[]> sortedEntropyResultLocal = entropyResultLocal.stream().sorted((e1, e2) -> {
				return (e1[0].compareTo(e2[0]));
			}).collect(Collectors.toList());

			DefectLocater.myCSV.writeToCsv(new File(entropyResultLocalPath), header, sortedEntropyResultLocal);
			//Map<String, Double> projectResults = new HashMap<String, Double>();
			//projectResults = Evaluation.EvaluateAndOutputEachCommitResult(entropyResultLocalPath, evaTestEachCommitCSV,
			//		0, bugNgram, evaCSVEachTrueBugCommit);
			
			//String[] outputProjectCSV = Evaluation.GenerateOutputForProject(project, projectResults, headers);
			//modelResults.add(outputProjectCSV);
			
			//System.out.println("Project: " + outputProjectCSV[0] + "; Top5: " + outputProjectCSV[3] + "; Mr2: "
				//	+ outputProjectCSV[4] + "; MAP:" + outputProjectCSV[5]);
		}

		//DefectLocater.myCSV.writeToCsv(new File(finalAllProjectsResultsLocal), headers, modelResults);

	}

}
