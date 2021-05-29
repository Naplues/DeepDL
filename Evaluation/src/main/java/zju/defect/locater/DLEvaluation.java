package zju.defect.locater;

import zju.defect.util.CSV_handler;
import zju.defect.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DLEvaluation {
//    	public static String[] projects = new String[] { "activemq", "closure-compiler", "deeplearning4j", "druid", "flink", "graylog2-server",
//                "jenkins", "jetty.project", "jitsi", "jmeter", "libgdx","robolectric", "storm"};

    public static String[] projects = new String[] {"h2o"};

    public static String root = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/";
    public static CSV_handler myCSV = new CSV_handler();

    public static double trainRatio = 0.6;
    public static double testRatio = 0.4;
    public static String modelType = "Transformer";

    public static Map<String, Map<String, String>> allProjectLocalPaths = new HashMap<String, Map<String, String>>();

    public static void main(String[] args) throws IOException {
        RunOurmethod();
    }

    private static void RunOurmethod() throws IOException {

        modelEvaluation(modelType);

    }

    private static void modelEvaluation(String modelType)
            throws IOException {

        List<String[]> modelResults = new ArrayList<String[]>();


		String finalAllProjectsResults = root + "h2o.csv";

        String[] headers = new String[] { "Project", "aveTop1Accuracy", "aveTop2Accuracy", "aveTop5Accuracy",
                "aveTop10Accuracy", "aveAucec5Accuracy", "aveAucec20Accuracy", "aveAucec50Accuracy", "aveMr2TrueBugs",
                "aveMapTrueBugs", "allCommits", "allBugCommits", "allCleanCommits" };
        for (int i = 0; i < projects.length; i++) {
            String thisProject = projects[i];


//            String testSetPathCSV = root + thisProject + "/" + modelType + "/" + thisProject + "_test.csv";
//            String testSetPathJava = root + thisProject + "/" + modelType + "/" + thisProject + "_test.java";
//
//            String generateJitFeatures = root +  thisProject + "/"+ thisProject + ".csv";
//
//            List<String[]> jitFeaturesList = myCSV.getContentFromFile(new File(generateJitFeatures));  // sorted in commit time in descending order
//
//            int allCommits = jitFeaturesList.size();
//            int testSize = (int) Math.round(allCommits * testRatio);
//
//
//            Set<String> trainCommits = new HashSet<String>();
//            Set<String> testCommits = new HashSet<String>();
//            for (int j = 0; j < allCommits; j++) {
//                String[] thisCommit = jitFeaturesList.get(j);
//                String thisCommitId = thisCommit[0];
//                if (j < testSize)
//                    testCommits.add(thisCommitId);
//                else {
//                    trainCommits.add(thisCommitId);
//                }


//            String testSetPathCSV = root + thisProject + "/" + modelType + "/" + thisProject + "_newLMtest.csv";
            String testSetPathCSV = root + thisProject + "/" + modelType + "/" + thisProject + "_avg.csv";

            String inputAddLines = root +  thisProject + "/" + thisProject + "_add.csv"; // provided by raszz

//            String entropyTestResult = root + thisProject + "/" + thisProject + "_entropyTestResult"
//                    + modelType+ ".csv";


//            List<String[]> addLines = myCSV.getContentFromFile(new File(inputAddLines));
//            List<String[]> resultLines = myCSV.getContentFromFile(new File(entropyTestResult));



//            List<String> commitList = new ArrayList<>();
//
//            String tempCommitHash = addLines.get(0)[0];
//
//            commitList.add(tempCommitHash);
//
//            for(int j = 1; j < addLines.size(); j++){
//                String[] thisCommit = addLines.get(j);
//                String thisCommitHash = thisCommit[0];
//                if(!tempCommitHash.equals(thisCommitHash)){
//                    commitList.add(thisCommitHash);
//                    tempCommitHash = thisCommitHash;
//                }
//            }
//
//
////			int allCommits = jitFeaturesList.size();
//            int allCommits = commitList.size();
//
//
//            int testSize = (int) Math.round(allCommits * testRatio);
//            int trainSize = allCommits  - testSize;
//
//
//
//            Set<String> trainCommits = new HashSet<String>();
//            Set<String> testCommits = new HashSet<String>();
//
//            for (int j = 0; j < allCommits; j++) {
////				String[] thisCommit = jitFeaturesList.get(j);
////				String thisCommitId = thisCommit[0];
//                String thisCommitId = commitList.get(j);
//                if (j < testSize)
//                    testCommits.add(thisCommitId);
//                else {
//                    trainCommits.add(thisCommitId);
//                }
//            }
//
//            String[] header = myCSV.getHeaderFromFile(new File(inputAddLines));
//            String[] extendedString = new String[] { "Entropy" };
//            String[] testHeader = FileUtil.combineStringVector(header, extendedString);
//
//            List<String> testLineContent = new ArrayList<String>();
//            List<String[]> testLines = new ArrayList<String[]>();
//            // double filteredLines = 0;
//            Set<String> findTestCommits = new HashSet<String>();
//            double find = 0;
//            int totalTestLines = 0;
//            // for (int i = addLines.size() - 1; i >= 0; i--) {
//            for (int j = 0; j < resultLines.size(); j++) {
//                String[] thisLine =resultLines.get(j);
//                String lineContent = thisLine[1].trim();
//                String commit_hash = thisLine[0];
//                if (testCommits.contains(commit_hash)) {
//                    if (lineContent.length() >= 10) {
//                        String preprocessedLineContent = FileUtil.PreprocessCode(lineContent);
//                        testLineContent.add(preprocessedLineContent);
//                        testLines.add(thisLine);
//                        totalTestLines++;
//                    }
//                }
//            }
//            System.out.println(totalTestLines);
//            myCSV.writeToCsv(new File(testSetPathCSV), testHeader, testLines);
//            double validSize = FileUtil.GetCommitSizeOfMultiLines(testLines);




//			String evaTestEachCommitCSV = root +  thisProject + "/" + thisProject
//                    + "_eachCommitResult.csv";

            String evaTestEachCommitCSV = root +  thisProject + "/" + thisProject
                    + "_avg_result.csv";

//			String evaCSVEachTrueBugCommit = root + thisProject + "/" + thisProject
//					+ "_eachTrueBugCommitResult.csv";
            String evaCSVEachTrueBugCommit = root + thisProject + "/" + thisProject
                    + "_avg_true_result.csv";

//            String entropyTestResult = root + thisProject + "/" + thisProject + "_project_avg.csv";
            // 测试
            String entropyTestResult = root + thisProject + "/" + thisProject + "_avg.csv";




            Map<String, Double> projectResults = new HashMap<String, Double>();
            projectResults = Evaluation.EvaluateAndOutputEachCommitResult(entropyTestResult, evaTestEachCommitCSV,
                    modelType, evaCSVEachTrueBugCommit);
//            projectResults = Evaluation.EvaluateAndOutputEachCommitResult(testSetPathCSV, evaTestEachCommitCSV,
//                    modelType, evaCSVEachTrueBugCommit);


            String[] outputProjectCSV = Evaluation.GenerateOutputForProject(thisProject, projectResults, headers);
            modelResults.add(outputProjectCSV);

        }
        modelResults = FileUtil.AddAverage(modelResults);

        myCSV.writeToCsv(new File(finalAllProjectsResults), headers, modelResults);
    }

}
