package zju.defect.locater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import zju.defect.util.CSV_handler;
import zju.defect.util.FileUtil;

public class PreprocessLocal {

	public static void Run(String modelType) throws IOException {

		for (int i = 0; i < DefectLocater.projects.length; i++) {
			String thisProject = DefectLocater.projects[i];
			String inputGuruReport = DefectLocater.root + thisProject + "//" + thisProject + "cbs.csv";
			String inputAddLines = DefectLocater.root + thisProject + "//" + thisProject + "_add_fixed.csv";
			String inputDelLines = DefectLocater.root + thisProject + "//" + thisProject + "_del_fixed.csv";
			String trainSetPathRoot = DefectLocater.root + thisProject + "//local//";

			List<String[]> guruReport = DefectLocater.myCSV.getContentFromFile(new File(inputGuruReport));

			int allCommits = guruReport.size();

			int valSize = (int) Math.round(allCommits * DefectLocater.valRatio);
			int testSize = (int) Math.round(allCommits * DefectLocater.testRatio);
			int trainSize = allCommits - valSize - testSize;
			// String[] testEnd = guruReport.get(testSize-1);
			// String testEndTime = testEnd[6];
			Random rand = new Random();

			List<String> trainAndValCommits = new ArrayList<String>();
			Set<String> trainCommits = new HashSet<String>();
			Set<String> valCommits = new HashSet<String>();
			Set<String> testCommits = new HashSet<String>();

			for (int j = 0; j < allCommits; j++) {
				String[] thisCommit = guruReport.get(j);
				String thisCommitId = thisCommit[0];
				if (j < testSize)
					testCommits.add(thisCommitId);
				else {
					trainAndValCommits.add(thisCommitId);
				}
			}

			Set<Integer> valCommitsIndex = new HashSet<Integer>();
			// random generate validate set
			while (valCommitsIndex.size() < valSize) {
				int randIndex = rand.nextInt(trainAndValCommits.size());
				valCommitsIndex.add(randIndex);
			}

			for (int m = 0; m < trainAndValCommits.size(); m++) {
				String thisCommit = trainAndValCommits.get(m);
				if (valCommitsIndex.contains(m)) {
					valCommits.add(thisCommit);
				} else {
					trainCommits.add(thisCommit);
				}
			}

			System.out.println("Input sizes for " + thisProject + "; all Commits: " + allCommits + ";  trainsize: "
					+ trainCommits.size() + "; valsize: " + valCommits.size() + "test Commits: " + testCommits.size());
			DataPreprocessForTrainLocal(thisProject, inputAddLines, inputDelLines, trainSetPathRoot,
					trainCommits, modelType);
			 DataPreprocessForTestLocal(thisProject, inputAddLines, trainSetPathRoot, testCommits, modelType);
			// double validTestSize = DataPreprocessForTest(inputAddLines,
			// testSetPathCSV, testSetPathJava, testCommits);
			// System.out.println("Valid Sizes for " + thisProject + "; train
			// size: " + validTrainSize + "; valsize: "
			// + validValSize + "; test size: " + validTestSize);
		}
	}

	private static void DataPreprocessForTestLocal(String thisProject, String inputAddLines, String testSetPathRoot,
			Set<String> testCommits, String modelType) throws IOException {

		List<String[]> addLines = DefectLocater.myCSV.getContentFromFile(new File(inputAddLines));
		String[] header = DefectLocater.myCSV.getHeaderFromFile(new File(inputAddLines));
		String[] extendedString = new String[] { "Entropy" };
		String[] testHeader = FileUtil.combineStringVector(header, extendedString);

		GenerateTestsetByCommits(thisProject, addLines, testSetPathRoot, testHeader, testCommits, modelType);
	}
	
	private static void GenerateTestsetByCommits(String thisProject, List<String[]> addLines, String testSetPathRoot, String[] testHeader, Set<String> testCommits, String modelType) throws IOException {
	
		Map<String, List<String[]>> localTestlines = new HashMap<String, List<String[]>>();
		Map<String, List<String>> localTestlineContents = new HashMap<String, List<String>>();
		
		Map<String, String> testTrainPaths = DefectLocater.allProjectLocalPaths.get(thisProject);
		String trainGlobal  = DefectLocater.root + thisProject + "//CleanNgram//" + thisProject + "_trainCleanNgram.java";
		int testSizeIncluded = 0;
		int newModule = 0;
		int trainModule = testTrainPaths.keySet().size();
		for (int i = 0; i < addLines.size(); i++) {
		//for (int i = addLines.size()-1; i >=0; i--) {	
			String[] thisLine = addLines.get(i);
			String lineContent = thisLine[1].trim();
			String commit_hash = thisLine[0];
			
			String filePath = thisLine[3];
			int lastDirIndex = filePath.lastIndexOf("/");
			int firstDirIndex = filePath.indexOf("/");		
			String module="";
			String dir="";
			String subDir = "";
			String fileName = "";
			if (firstDirIndex > -1)
			  module = filePath.substring(0, firstDirIndex).toLowerCase();
			else
			  module = "root";
			if (lastDirIndex > -1){
			  dir = filePath.substring(0, lastDirIndex).toLowerCase();
			  subDir = dir.substring(dir.lastIndexOf("/")+1, dir.length()).toLowerCase();
			  fileName = filePath.substring(lastDirIndex, filePath.length());
			  fileName = fileName.substring(0, fileName.lastIndexOf(".java")).toLowerCase();
			}
			else{
				subDir=filePath.substring(0,filePath.lastIndexOf(".java")).toLowerCase();
			}
			//String thisTestPathJava = testSetPathRoot + module + "//"+ subDir + "_test.java";
			//String thisTestPathCSV = testSetPathRoot + module + "//"+ subDir + "_test.csv";
			//String thisTestPathJava = testSetPathRoot + module +  "_test.java";
			//String thisTestPathCSV = testSetPathRoot + module + "_test.csv";

			String thisTestPathJava = "";
			String thisTestPathCSV = "";
			
			if (modelType.equals("LocalCleanNgramFile")){
				thisTestPathJava = testSetPathRoot + module + "//"+  dir.replace("/", "_") + "_" + fileName+ "_test.java";
				thisTestPathCSV = testSetPathRoot + module + "//"+  dir.replace("/", "_") + "_" +fileName + "_test.csv";
			}
			
			if (modelType.equals("LocalCleanNgramModule")){
				thisTestPathJava = testSetPathRoot + module + "_test.java";
				thisTestPathCSV = testSetPathRoot + module + "_test.csv";
			}	
			//String thisTestPathJava = testSetPathRoot + module + "//"+ dir.replace("/", "_") + "_" + fileName + "_test.java";
			//String thisTestPathCSV = testSetPathRoot + module + "//"+ dir.replace("/", "_") + "_" + fileName + "_test.csv";
			
			if (!testTrainPaths.containsKey(thisTestPathJava)){
				testTrainPaths.put(thisTestPathJava, trainGlobal);
				newModule++;
			}
			
			if (!localTestlines.containsKey(thisTestPathCSV)) {
				List<String> testLinesContent = new ArrayList<String>();
				List<String[]> testLines = new ArrayList<String[]>();
				localTestlines.put(thisTestPathCSV, testLines);
				localTestlineContents.put(thisTestPathJava, testLinesContent);
			}
			// if (testCommits.contains(commit_hash)){
			if (testCommits.contains(commit_hash)) {
				if (lineContent.length() >= 10) {
					localTestlines.get(thisTestPathCSV).add(thisLine);
					String preprocessedLineContent = FileUtil.PreprocessCode(lineContent);
					localTestlineContents.get(thisTestPathJava).add(preprocessedLineContent);
					testSizeIncluded++;
				}
			}
		}
		DefectLocater.allProjectLocalPaths.put(thisProject, testTrainPaths);
		
		// TODO: 对比testTrainPaths 和 localTestlineContents 中的 key，查看区别
/*		int size1 = 0;
		for(String key:localTestlineContents.keySet()){
			size1++;
			if (!testTrainPaths.containsKey(key))
				System.out.println("key"+"11111111");
		}
		int size2=0;
		for(String key:testTrainPaths.keySet()){
			size2++;
			if (!localTestlineContents.containsKey(key))
				System.out.println("key"+"11111111");
		}
        System.out.println(testSizeIncluded + "; number of commits:"+testCommits.size());*/
		// Set<String> notFoundCommits = FileUtil.SubSetCommits(testCommits,
		// findTestCommits);
		int testLineContentsCount = 0;
		int testLineContentsCountJava2 = 0;
		Map<String,Integer> pathLinesCountForTest = new HashMap<String, Integer>();
		for(String key:localTestlineContents.keySet()){
			File fileDir = new File(key.substring(0,key.lastIndexOf("/"))); 
			List<String> lineContents = localTestlineContents.get(key);
			testLineContentsCount = testLineContentsCount + lineContents.size();
			
			if (!fileDir.exists())
			   fileDir.mkdirs(); 
			FileUtil.writeLinesToFile(localTestlineContents.get(key), key);	
			
			List<String> testLines2 = FileUtil.readLinesFromFile(key);			
			testLineContentsCountJava2 = testLineContentsCountJava2 + testLines2.size();
			pathLinesCountForTest.put(key, testLines2.size());
		}
		
		//System.out.println(testLineContentsCount);
		//System.out.println(testLineContentsCountJava2);
		
		int testLineContentsCount2 = 0;
		int testLineContentsCountWrited = 0;
		for(String key:localTestlines.keySet()){  		    
			File fileDir = new File(key.substring(0,key.lastIndexOf("/"))); 
			List<String[]> lines = localTestlines.get(key);	
		
			testLineContentsCount2 = testLineContentsCount2 + lines.size();
			
			if (!fileDir.exists())
			  fileDir.mkdirs(); 
			CSV_handler myCSV = new CSV_handler();				
			myCSV.writeToCsv(new File(key), testHeader, localTestlines.get(key));
			
			List<String[]> writedLines = myCSV.getContentFromFile(new File(key));
			testLineContentsCountWrited = testLineContentsCountWrited + writedLines.size();
			//return trainValidCommits;
		}
		//System.out.println(testLineContentsCount2);
		//System.out.println(testLineContentsCountWrited);
		
		Map<String,String> testTrainPathForTest = DefectLocater.allProjectLocalPaths.get(thisProject);
		int countByMap = 0;
		Map<String,Integer> pathLinesCount = new HashMap<String, Integer>();
		for(String key:testTrainPathForTest.keySet()){  		    
			List<String> testLines = FileUtil.readLinesFromFile(key);	
			pathLinesCount.put(key, testLines.size());
			countByMap = countByMap + testLines.size();
			//return trainValidCommits;
		}

		
		for(String key:testTrainPathForTest.keySet()){
			int linesInMap = pathLinesCount.get(key);
			int linesInLocalLinesContent = pathLinesCountForTest.get(key);

			if (linesInMap != linesInLocalLinesContent)
				System.out.println(key);
		}
		
		//System.out.println(countByMap);
		System.out.println("---------"+thisProject+" Train Module:" + trainModule + "; New Module: " + newModule);
		//DefectLocater.myCSV.writeToCsv(new File(testSetPathCSV), testHeader, testLines);
		// System.out.println("input val Size:"+ testCommits.size());
		//double validSize = FileUtil.GetCommitSizeOfMultiLines(testLines);
		// System.out.println("generate val Size:"+ generateSize);
		// System.out.println("find:"+ find);
		//FileUtil.writeLinesToFile(testLineContent, testSetPathJava);
	}
	
	private static void DataPreprocessForTrainLocal(String thisProject, String inputAddLines, String inputDelLines,
			String trainSetPathRoot, Set<String> trainCommits, String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> delLines = DefectLocater.myCSV.getContentFromFile(new File(inputDelLines));
		List<String[]> addLines = DefectLocater.myCSV.getContentFromFile(new File(inputAddLines));

	    if (modelType.equals("LocalCleanNgramModule")||modelType.equals("LocalCleanNgramFile")){
			String[] header = DefectLocater.myCSV.getHeaderFromFile(new File(inputAddLines));
			GenerateTrainsetByCommitsLocal(thisProject, addLines, trainSetPathRoot, trainCommits, header, modelType);
		}
	}

	public static void GenerateTrainsetByCommitsLocal(String thisProject, List<String[]> addLines, String trainSetPathRoot,
			Set<String> trainCommits, String[] header, String modelType) throws IOException {
		// TODO Auto-generated method stub

		//Map<String, String> dirs = new HashMap<String, String>();
		//Map<Integer, String> lineLocal = new HashMap<Integer, String>();
		Map<String, List<String[]>> localTrainlines = new HashMap<String, List<String[]>>();
		Map<String, List<String>> localTrainlineContents = new HashMap<String, List<String>>();
		Map<String, String> testTrainPaths = new HashMap<String, String>();
			
		for (int i = 0; i < addLines.size(); i++) {
			String[] thisLine = addLines.get(i);
			String filePath = thisLine[3];			

			int lastDirIndex = filePath.lastIndexOf("/");
			int firstDirIndex = filePath.indexOf("/");
			String module="";
			String dir="";
			String subDir = "";
			String fileName = "";
			if (firstDirIndex > -1)
			  module = filePath.substring(0, firstDirIndex).toLowerCase();
			else
			  module = "root";
			if (lastDirIndex > -1){
			  dir = filePath.substring(0, lastDirIndex).toLowerCase();
			  subDir = dir.substring(dir.lastIndexOf("/")+1, dir.length()).toLowerCase();
			  fileName = filePath.substring(lastDirIndex, filePath.length());
			  fileName = fileName.substring(0, fileName.lastIndexOf(".java")).toLowerCase();
			}
			else{		
				subDir=filePath.substring(0,filePath.lastIndexOf(".java")).toLowerCase();
			}
			//String thisTrainPathJava = trainSetPathRoot + module + "//"+ subDir + "_train.java";
			//String thisTrainPathCSV = trainSetPathRoot + module + "//"+ subDir + "_train.csv";
			String thisTrainPathJava = "";
			String thisTrainPathCSV = "";
			
			if (modelType.equals("LocalCleanNgramFile")){
			  thisTrainPathJava = trainSetPathRoot + module + "//"+  dir.replace("/", "_") + "_" + fileName+ "_train.java";
			  thisTrainPathCSV = trainSetPathRoot + module + "//"+  dir.replace("/", "_") + "_" +fileName + "_train.csv";
			}
			
			if (modelType.equals("LocalCleanNgramModule")){
			  thisTrainPathJava = trainSetPathRoot + module + "_train.java";
			  thisTrainPathCSV = trainSetPathRoot + module + "_train.csv";
			}				
			
			String lineContent = thisLine[1].trim();
			String commit_hash = thisLine[0];
			String label = thisLine[7];

			if (!localTrainlines.containsKey(thisTrainPathCSV)) {
				List<String> trainLinesContent = new ArrayList<String>();
				List<String[]> trainLines = new ArrayList<String[]>();
				localTrainlines.put(thisTrainPathCSV, trainLines);
				localTrainlineContents.put(thisTrainPathJava, trainLinesContent);
				//testTrainPaths.put(trainSetPathRoot + module + "//"+ subDir + "_test.java", thisTrainPathJava);
				if (modelType.equals("LocalCleanNgramFile"))
				  testTrainPaths.put(trainSetPathRoot + module + "//"+ dir.replace("/", "_") + "_" +fileName + "_test.java", thisTrainPathJava);
				if (modelType.equals("LocalCleanNgramModule"))
				  testTrainPaths.put(trainSetPathRoot + module + "_test.java", thisTrainPathJava);
			}
			
			if (modelType.equals("LocalCleanNgramModule") || modelType.equals("LocalCleanNgramFile")){
				if (trainCommits.contains(commit_hash) && label.toLowerCase().equals("false") && lineContent.length() >= 10) {
					localTrainlines.get(thisTrainPathCSV).add(thisLine);
					String preprocessedLineContent = FileUtil.PreprocessCode(lineContent);
					localTrainlineContents.get(thisTrainPathJava).add(preprocessedLineContent);
				} 
			}
		}
		
		DefectLocater.allProjectLocalPaths.put(thisProject, testTrainPaths);
		
		for(String key:localTrainlineContents.keySet()){  		    
			File fileDir = new File(key.substring(0,key.lastIndexOf("/"))); 
			if (!fileDir.exists())
			  fileDir.mkdirs();
			
			List<String> lineContents = localTrainlineContents.get(key);
			FileUtil.writeLinesToFile(lineContents, key);						
			//double trainValidCommits = FileUtil.GetCommitSizeOfMultiLines(trainLines);
			//return trainValidCommits;
		}		
		
		for(String key:localTrainlines.keySet()){  		    
			File fileDir = new File(key.substring(0,key.lastIndexOf("/"))); 
			if (!fileDir.exists())
			  fileDir.mkdirs(); 
			CSV_handler myCSV = new CSV_handler();			
			myCSV.writeToCsv(new File(key), header, localTrainlines.get(key));
			//double trainValidCommits = FileUtil.GetCommitSizeOfMultiLines(trainLines);
			//return trainValidCommits;
		}
		
		
	}

}
