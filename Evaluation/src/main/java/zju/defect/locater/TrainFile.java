package zju.defect.locater;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import zju.defect.util.FileUtil;


public class TrainFile {
    private static FileUtil fileUtil = new FileUtil();
    private static long[] learnStats = new long[2];

    public static void cleanFile(File file, List<String> bugFiles, String stored){
        List<String> trainLineContent = new ArrayList<String>();
        learnStats = new long[] { 0, -System.currentTimeMillis() };
        try {
            Files.walk(file.toPath())
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .filter(f -> f.getName().endsWith(".java"))
//                    .filter(Files -> excludeBugFile(Files, bugFiles))
                    .forEach(f -> storeFile(f.toString(), trainLineContent));
//                    .forEach(System.out::println);
            FileUtil.writeLinesToFile(trainLineContent, stored);
        } catch (IOException e){
            e.printStackTrace();
        }
    }



    public static boolean excludeBugFile(File file, List<String> bugFiles){
        boolean exclude = true;
        for(String bugFile : bugFiles) {
            if(file.getPath().equals(bugFile)){
                exclude = false;
            }
        }
        return exclude;
    }

    public static int[] storeFile(String filePath, List<String> trainLineContent){
        List<String> lines = FileUtil.readLinesFromFile(filePath);
        int[] lineNum = new int[lines.size()];
        boolean inBlock = false;
        StringBuilder newline = new StringBuilder();
        int j = 0;
        int l = 0;
        for (String line : lines){
            line = line.trim();
            int i = 0;
            char[] chars = line.toCharArray();
            if (!inBlock) newline = new StringBuilder();
            while (i < line.length()) {
                if (!inBlock && i+1 < line.length() && chars[i] == '/' && chars[i+1] == '*') {
                    inBlock = true;
                    i++;
                } else if (inBlock && i+1 < line.length() && chars[i] == '*' && chars[i+1] == '/') {
                    inBlock = false;
                    i++;
                } else if (!inBlock && i+1 < line.length() && chars[i] == '/' && chars[i+1] == '/') {
                    break;
                } else if (!inBlock) {
                    newline.append(chars[i]);
                }
                i++;
            }
            lineNum[j] = -1;
            if (!inBlock && newline.length() > 0) {
                trainLineContent.add(new String(newline));
                lineNum[j] = l;
                l++;
            }
            j++;
//            String codeLine = StringEscapeUtils.unescapeJava(line);
//            String preprocessedLineContent = FileUtil.PreprocessCode(codeLine.trim());
//            String preprocessedLineContent = codeLine.trim();
//            trainLineContent.add(preprocessedLineContent);
        }
        return lineNum;
    }

    public List<List<String>> getTestFile(List<String> testFiles, String storePath, String mappingPath, String commitHash){
        List<List<String>> AllMapping = new ArrayList<>();
        for(String testFilePath : testFiles){
            List<String> testLineContent = new ArrayList<>();
            int[] lineNum = storeFile(testFilePath, testLineContent);
            List<String> lineNumMapping = new ArrayList<>();
            for(int i = 0; i < lineNum.length; i++){
                lineNumMapping.add(Integer.toString(lineNum[i]));
            }

            String testLineStored = storePath + "/" + fileUtil.setFileName(testFilePath, commitHash);
            String lineNumStored = mappingPath + "/" + fileUtil.setFileName(testFilePath, commitHash).substring(0, fileUtil.setFileName(testFilePath, commitHash).length()-5)+"_Mapping";
            FileUtil.writeLinesToFile(testLineContent, testLineStored);
            FileUtil.writeLinesToFile(lineNumMapping, lineNumStored);
            AllMapping.add(lineNumMapping);
        }
        return AllMapping;
    }






    public static void main(String args[]){
//        File file = new File("/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/project/storm");
//        List<String> bugFiles = null;
//        String stored = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/storm/Storm_train.java";
//        cleanFile(file, bugFiles, stored);

//        String root = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm";
//        String[] projects = new String[] { "closure-compiler", "deeplearning4j", "druid", "flink", "graylog2-server", "jenkins"
//        ,"jetty.project", "jitsi", "jmeter", "libgdx", "robolectric"};
//        for(String project : projects){
//            String projectPath = root + "/project/" + project;
//            String storedPath = root + "/dataset/" + project + "/" + project + "_train.java";
//            File file = new File(projectPath);
//            List<String> bugFiles = null;
//            cleanFile(file, bugFiles, storedPath);
//        }

//        File file = new File("/Users/lifeasarain/Desktop/ActiveMQConnectionConsumer.java");
        // 需要提取java文件的项目路径
        File file = new File("/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/project/h2o-3");
        List<String> bugFiles = null;
//        String stored = "/Users/lifeasarain/Desktop/test.java";
        // 提取出的项目储存路径
        String stored = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/h2o/h2o_train_og.java";
        cleanFile(file, bugFiles, stored);


//        File file = new File("/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/project/activemq");
//        List<String> bugFiles = null;
//        String stored = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/activemq1710/activemq_test.java";
//        cleanFile(file, bugFiles, stored);


//        String remove = "/** DruidStatManagerFacade#getDataSourceStatDataList 该方法可以获取所有数据源的监控 **/";
//        String preprocessedLineContent = StringEscapeUtils.unescapeJava(remove);
//        preprocessedLineContent = FileUtil.PreprocessCode(preprocessedLineContent.trim());
//        System.out.print(preprocessedLineContent);
    }
}

/*
sss
 */