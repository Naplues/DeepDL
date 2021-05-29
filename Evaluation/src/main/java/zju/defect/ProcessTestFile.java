package zju.defect;
import zju.defect.locater.TrainFile;
import zju.defect.util.FileUtil;
import zju.defect.util.GitUtil;
import zju.defect.util.CSV_handler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static zju.defect.util.FileUtil.readLinesFromFile;

public class ProcessTestFile {
    private static FileUtil fileUtil = new FileUtil();
    private static CSV_handler myCSV = new CSV_handler();
    private static GitUtil gitUtil = new GitUtil();
    private static TrainFile trainFile = new TrainFile();
    private static String root = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm";
    public static String[] projects = new String[] { "closure-compiler", "deeplearning4j", "druid", "flink", "graylog2-server", "jenkins"
            ,"jetty.project", "jitsi", "libgdx", "robolectric"};

    private List<Map<String, List<String>>> collectTestSet(String repoPath, String addCsvPath, String project) throws IOException {
        List<String[]> addLines = myCSV.getContentFromFile(new File(addCsvPath));
        String commitHash = "";
        String filePath = "";
        List<String> commitFiles = new ArrayList<>();
        // 所有的测试文件
        List<Map<String, List<String>>> fileLines = new ArrayList<>();
        Map<String, List<String>> fileLineMap = new HashMap<>();
        List<String> lineNums = new ArrayList<>();
        for (int i = 0; i < addLines.size(); i++) {
            String[] thisLine = addLines.get(i);

            // 修改的commit
            String currentCommit = thisLine[0];
            // 修改的文件

            String currentFilePath = thisLine[3];
            // 修改的代码行
            String lineNum = thisLine[4];
            // Bug label
            String currentLabel = thisLine[8].toLowerCase();

//            if(currentLabel.equals("false")){
//                continue;
//            }

            // 确定首个commit
            if(0 == i){
                commitHash = currentCommit;
                filePath = currentFilePath;
                gitUtil.resetRepo(repoPath, commitHash);
                lineNums.add(lineNum);
                fileLineMap.put(repoPath+"/"+currentFilePath, lineNums);
                commitFiles.add(repoPath+"/"+currentFilePath);
                continue;
            }

            // 处理最后一个commit
            else if(addLines.size()-1 == i){
                lineNums.add(lineNum);
                fileLineMap.put(repoPath+"/"+currentFilePath, lineNums);
                commitFiles.add(repoPath+"/"+currentFilePath);
//                trainFile.getTestFile(commitFiles, root+"/dataset/test/activemq/testdataset", root+"/dataset/test/activemq/linenummapping", commitHash);
                trainFile.getTestFile(commitFiles, root+"/dataset/test/"+project+"/testdataset", root+"/dataset/test/"+project+"/linenummapping", commitHash);

            }

            // 同一个commit 同一个文件
            else if(commitHash.equals(currentCommit) && filePath.equals(currentFilePath)){
                lineNums.add(lineNum);
                continue;
            }

            // 同一个commit 不同文件
            else if(commitHash.equals(currentCommit) && !filePath.equals(currentFilePath)){
                fileLineMap.put(repoPath+"/"+currentFilePath, lineNums);
                commitFiles.add(repoPath+"/"+currentFilePath);
                fileLines.add(fileLineMap);
                filePath = currentFilePath;
                continue;
            }

            // 不同的commit， 先处理上一个commit，再重定位到下一个commit
            else if(!commitHash.equals(currentCommit)){
//                fileLineMap.put(repoPath+"/"+filePath, lineNums);
//                commitFiles.add(repoPath+"/"+filePath);
//                trainFile.getTestFile(commitFiles, root+"/dataset/test/activemq/testdataset", root+"/dataset/test/activemq/linenummapping", commitHash);
                trainFile.getTestFile(commitFiles, root+"/dataset/test/"+project+"/testdataset", root+"/dataset/test/"+project+"/linenummapping", commitHash);
                // 初始化
                lineNums.clear();
                fileLineMap.clear();
                commitFiles.clear();

                lineNums.add(lineNum);
                fileLineMap.put(repoPath+"/"+currentFilePath, lineNums);
                commitFiles.add(repoPath+"/"+currentFilePath);

                // 重定位到下一个commit
                gitUtil.resetRepo(repoPath, currentCommit);
                commitHash = currentCommit;
                filePath = currentFilePath;
            }
        }
        return fileLines;
    }

    private void processResult(String addCsvPath, String resultPath) throws IOException{
        List<String[]> addLines = myCSV.getContentFromFile(new File(addCsvPath));
        for (int i = 0; i < addLines.size(); i++) {
            String[] thisLine = addLines.get(i);
            String currentCommit = thisLine[0];
            String currentFilePath = thisLine[3];
            String currentLineNum = thisLine[4];

            String resultName = fileUtil.setFileName(currentFilePath, currentCommit).substring(0, fileUtil.setFileName(currentFilePath, currentCommit).length()-5);
            String entropyFile = resultName + ".txt";
            String mappingFile = resultName + "_Mapping";

            List<String> mapping = readLinesFromFile(mappingFile);


        }
    }

    public void getContentSet(String addCsvPath, String project) throws IOException{
        List<String[]> addLines = myCSV.getContentFromFile(new File(addCsvPath));
        String commitHash = "";
        String filePath = "";
        List<String> commitFiles = new ArrayList<>();
        // 所有的测试文件

        List<String> lineNums = new ArrayList<>();

        for (int i = 0; i < addLines.size(); i++) {
            String[] thisLine = addLines.get(i);

            // 修改的commit
            String currentCommit = thisLine[0];
            // 修改的文件
            String currentFilePath = thisLine[3];
            // 修改的代码行
            String lineNum = thisLine[4];
            // Bug label
            String currentLabel = thisLine[8].toLowerCase();



            String currentFile = fileUtil.setFileName(currentFilePath, commitHash);
            String currentMapping = fileUtil.setFileName(currentFilePath, commitHash).substring(0, fileUtil.setFileName(currentFilePath, commitHash).length()-5)+"_Mapping";
            List<String> fileLines = readLinesFromFile(currentFile);
            List<String> mappings = readLinesFromFile(currentMapping);

            int preOneLineNum = Integer.parseInt(lineNum) - 1;
            int preTwoLineNum = Integer.parseInt(lineNum) - 2;
            int exOneLineNum = Integer.parseInt(lineNum) - 1;
            int exTwoLineNum = Integer.parseInt(lineNum) - 2;

            String preOneLine = fileLines.get(Integer.parseInt(mappings.get(preOneLineNum)));
        
        }
    }


    // 重定位到ref

    // 提取修改文件，建立行之间的映射

    // Tokenize修改文件

    public static void main(String[] args) throws IOException{

        ProcessTestFile ptf = new ProcessTestFile();
//        String addCsvPath = "/Users/lifeasalibgdxrain/Desktop/tmp/SE/Plugin2/lstm/dataset/activemq/activemq1710_add.csv";

        String project = "libgdx";
        String addCsvPath = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/" + project + "/" + project + "_newadd.csv";

        String repoPath = root + "/project/" + project;
        ptf.collectTestSet(repoPath, addCsvPath, project);
    }
}
