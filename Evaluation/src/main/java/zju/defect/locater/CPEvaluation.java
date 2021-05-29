package zju.defect.locater;

import zju.defect.util.CSV_handler;
import zju.defect.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CPEvaluation {
//    	public static String[] projects = new String[] { "activemq", "closure-compiler", "deeplearning4j", "druid", "flink", "graylog2-server",
//                "jenkins", "jetty.project", "jitsi", "jmeter", "libgdx","robolectric", "storm"};

    public static String[] projects = new String[] {"closure-compiler_cp"};

    public static String root = "/Users/lifeasarain/PycharmProjects/OVCNLM/data/dataset/crossproject/";
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


		String finalAllProjectsResults = root + "closure-compiler_cp.csv";

        String[] headers = new String[] { "Project", "aveTop1Accuracy", "aveTop2Accuracy", "aveTop5Accuracy",
                "aveTop10Accuracy", "aveAucec5Accuracy", "aveAucec20Accuracy", "aveAucec50Accuracy", "aveMr2TrueBugs",
                "aveMapTrueBugs", "allCommits", "allBugCommits", "allCleanCommits" };
        for (int i = 0; i < projects.length; i++) {
            String thisProject = projects[i];

//            String testSetPathCSV = root + thisProject + "/" + modelType + "/" + thisProject + "_project_avg.csv";
//
//            String inputAddLines = root +  thisProject + "/" + thisProject + "_add.csv"; // provided by raszz
//


            String evaTestEachCommitCSV = root +  thisProject + "/" + thisProject
                    + "_avg_result.csv";

//			String evaCSVEachTrueBugCommit = root + thisProject + "/" + thisProject
//					+ "_eachTrueBugCommitResult.csv";
            String evaCSVEachTrueBugCommit = root + thisProject + "/" + thisProject
                    + "_avg_true_result.csv";

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
