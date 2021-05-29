package zju.defect;

import zju.defect.util.CSV_handler;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BugCommit {
    private static CSV_handler myCSV = new CSV_handler();

    private static Set<String> getBuggyCommits(String addLines, String newAddLines) throws IOException {
        // TODO Auto-generated method stub
        Set<String> buggyCommitIds = new HashSet<String>();
        List<String[]> addLinesList = myCSV.getContentFromFile(new File(addLines));
        List<String[]> newAddLinesList = new LinkedList<>();

        for (int i=0; i<addLinesList.size();i++) {
            String[] thisLine = addLinesList.get(i);
            String id = thisLine[0];
            String label = thisLine[8].toLowerCase();

            if(label.equals("true")){
                newAddLinesList.add(thisLine);
            }

            if (label.equals("true") && !buggyCommitIds.contains(id))
                buggyCommitIds.add(id);
        }

        String[] headersForCommits = new String[] { "commit_hash", "content", "file_pre", "file_new", "line_num", "author", "time", "bug_introducing", "commit_label"};
        myCSV.writeToCsv(new File(newAddLines), headersForCommits, newAddLinesList);

        return buggyCommitIds;
    }

    public static void main(String[] args)throws IOException{
        String project = "robolectric";

        String addLines = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/"+project+"/"+project+"_add.csv";
        String newAddLines = "/Users/lifeasarain/Desktop/tmp/SE/Plugin2/lstm/dataset/"+project+"/"+project+"_newadd.csv";
        Set<String> buggyCommitIds = getBuggyCommits(addLines, newAddLines);
        System.out.print(buggyCommitIds.size());
    }
}
