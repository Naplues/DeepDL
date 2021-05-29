package zju.defect;

import slp.core.lexing.LexerRunner;
import slp.core.translating.Vocabulary;
import slp.core.translating.VocabularyRunner;

import java.io.File;
import java.util.List;

public class Tokenize {
    public static void lexFile(String sourcePath, String targetPath){
        File inDir = new File(sourcePath);
        File outDir = new File(targetPath);
        LexerRunner.preTranslate(false);
        boolean emptyVocab = Vocabulary.size() <= 1;
        LexerRunner.lexDirectory(inDir, outDir);
        if (emptyVocab) {
            File vocabFile = new File(outDir.getParentFile(), "train.vocab");
            VocabularyRunner.write(vocabFile);
        }
        else {
            System.err.println("Not enough arguments given."
                    + "Lexing requires at least two arguments: source and target path.");
        }
    }




//    public static void main(String args[]){
//
//        String root = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset";
//        String[] projects = new String[] { "closure-compiler", "deeplearning4j", "druid", "flink", "graylog2-server", "jenkins"
//                ,"jetty.project", "jitsi", "jmeter", "libgdx", "robolectric"};
//        for(String project : projects){
//            String trainSourcePath = root + "/" + project + "/" + project + "_train.java";
//            String trainTargetPath = root + "/" + project + "/" + project + "_train_tokenize.java";
//            String testSourcePath = root + "/" + project + "/" + project + "_test.java";
//            String testTargetPath = root + "/" + project + "/" + project + "_test_tokenize.java";
//            lexFile(trainSourcePath, trainTargetPath);
//            lexFile(testSourcePath, testTargetPath);
//        }
//
////        String sourcePath = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/activemq1710/activemq_test.java";
////        String targetPath= "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/activemq1710/activemq_test_tokenize.java";
////        lexFile(sourcePath, targetPath);
//
//
////        String sourcePath = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/activemq1710/activemq1710_test.java";
////        String targetPath= "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/activemq1710/activemq1710_test_tokenize.java";
////        lexFile(sourcePath, targetPath);
//    }
//    public static void main(String args[]){
////        String root = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/test/activemq/testdataset";
//        String project = "jetty.project";
//
//        String root = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/test/"+project+"/testdataset";
//        File dir = new File(root);
//        String [] files = dir.list();
//        for(String file : files){
//            String testSourcePath = root + "/" + file;
////            String testTargetPath = "/Users/lifeasaragraylog2-serverin/Desktop/tmp/SE/Plugin2/lstm/dataset/test/activemq/tokenized/" + file.substring(0, file.length()-5) +"_tk";
//            String testTargetPath = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/test/"+project+"/tokenized/" + file.substring(0, file.length()-5) +"_tk";
//            lexFile(testSourcePath, testTargetPath);
//        }
//    }

    public static void main(String args[]) {
        String root = "/Users/lifeasarain/PycharmProjects/OVCNLM/data/dataset/";
//        String[] projects = new String[] { "closure-compiler", "deeplearning4j", "druid", "flink",
//                "jenkins", "storm", "robolectric", "graylog2-server", "jetty.project",
//                "jitsi", "libgdx"};
        String[] projects = new String[] { "h2o"};

        for(String project : projects){
//            String sourcePath = root + project + "/" + project + "_all.java";
//            String targetPath = root + project + "/" + project + "_all_tk.java";
//            String sourcePath = root + project + "/" + project + "_context.java";
//            String targetPath = root + project + "/" + project + "_context_tk.java";

            // 测试
            String sourcePath = root + project + "/" + project + "_train_og.java";
            String targetPath = root + project + "/" + project + "_train_og_tk.java";


            lexFile(sourcePath, targetPath);
        }





    }



}
