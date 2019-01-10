package githubRepo;

import com.github.javaparser.ParseProblemException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javaParserHelper.JavaMethodVisitor;
import javaParserHelper.MethodStruct;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import restApi.github.GithubRestApiClient;
import restApi.github.GithubRestApiRepoFileHelper;
import restApi.github.GithubRestApiRepoHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class analyzes commits in a Github repository to detect changes among Java files.
 */
public class GithubRepoJavaFileAnalyzer {
    //region constant variables

    private static final String CSV_FILE_SUFFIX = "_methodAnalysis.csv";
    private static final String LAST_COMMIT_FILE_SUFFIX = "_lastCommitSha";

    private static final int DEFAULT_THREAD_COUNT = 1;

    //endregion


    //region variables

    private final GithubRestApiClient restApiClient;

    private final GithubRestApiRepoHelper repoHelper;
    private final GithubRestApiRepoFileHelper repoFileHelper;

    private int threadCount = DEFAULT_THREAD_COUNT;

    //endregion


    //region constructors

    public GithubRepoJavaFileAnalyzer(String repoFullName, String username, String password) {
        restApiClient = new GithubRestApiClient(username, password);
        restApiClient.setRepoFullName(repoFullName);

        repoHelper = restApiClient.getRepoHelper();
        repoFileHelper = repoHelper.getRepoFileHelper();
    }

    public GithubRepoJavaFileAnalyzer(String repoFullName) {
        this(repoFullName, "", "");
    }

    //endregion


    //region threadCount getter and setter

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    //endregion


    //region methods

    /**
     * This method analyzes the increases of Java files' methods' parameters
     * in the commits of master branch of the given Github repo.
     *
     * @param processedCommitLimit commit limit (here, 0 (zero) means no limit)
     * @throws Exception if any error occurs
     */
    public void analyzeJavaFileMethodParameterInMasterBranchCommits(final long processedCommitLimit) throws Exception {
        String fileNamePrefix = repoHelper.getRepoFullName().replace('/', '_');
        String csvFileName = fileNamePrefix + CSV_FILE_SUFFIX;
        String lastCommitFileName = fileNamePrefix + LAST_COMMIT_FILE_SUFFIX;

        // if the program is interrupted, it will restart from the last processed commit instead of the latest commit
        JsonObject currentCommit;
        PrintWriter csvPrintWriter;
        if (new File(lastCommitFileName).isFile()) {
            Scanner scanner = new Scanner(new FileReader(lastCommitFileName));
            currentCommit = repoHelper.getCommitInfo(scanner.nextLine());
            scanner.close();

            csvPrintWriter = new PrintWriter(new FileWriter(csvFileName, true), true);
        } else {
            currentCommit = repoHelper.getLatestCommitInfo();
            csvPrintWriter = new PrintWriter(new FileWriter(csvFileName), true);
            csvPrintWriter.println("Commit SHA,Java File,Old function signature,New function signature");
        }

        PrintWriter lastCommitPrintWriter = new PrintWriter(new FileWriter(lastCommitFileName), true);

        String currentCommitSha = GithubRestApiRepoHelper.getCommitSha(currentCommit);
        String previousCommitSha = GithubRestApiRepoHelper.getPreviousCommitSha(currentCommit);

        lastCommitPrintWriter = writeToFirstLine(currentCommitSha, lastCommitPrintWriter, lastCommitFileName);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        long processedCommitCount = 0L;
        while ((previousCommitSha != null) &&
                ((processedCommitLimit == 0L) || (processedCommitCount < processedCommitLimit))) {
            String consoleOutputBase = "Processing commit: " + currentCommitSha + " : ";
            System.out.print(consoleOutputBase);

            JsonArray currentCommitFiles = getModifiedJavaFiles(GithubRestApiRepoHelper.getCommitFiles(currentCommit));

            List<Future<OutputDataForEachFileName>> futureList = new ArrayList<>(currentCommitFiles.size());
            for (JsonElement jsonElement : currentCommitFiles) {
                final String fileName = GithubRestApiRepoFileHelper.getFileName(jsonElement.getAsJsonObject());
                final String currentCommitShaFinal = currentCommitSha;
                final String previousCommitShaFinal = previousCommitSha;

                futureList.add(executorService.submit(() ->
                        processEachFileName(fileName, currentCommitShaFinal, previousCommitShaFinal)));
            }

            int processedFileIndex = 0;
            for (Future<OutputDataForEachFileName> future : futureList) {
                OutputDataForEachFileName outputDataForEachFileName = future.get();

                writeToFile(csvPrintWriter, outputDataForEachFileName);

                printAndReplaceAtSameLineOfConsole(consoleOutputBase +
                        Long.toString(Math.round(((double) processedFileIndex) / currentCommitFiles.size() * 100.0)) + "%");
                processedFileIndex++;
            }

            currentCommit = repoHelper.getCommitInfo(previousCommitSha);
            currentCommitSha = GithubRestApiRepoHelper.getCommitSha(currentCommit);
            previousCommitSha = GithubRestApiRepoHelper.getPreviousCommitSha(currentCommit);

            lastCommitPrintWriter = writeToFirstLine(currentCommitSha, lastCommitPrintWriter, lastCommitFileName);

            printAndReplaceAtSameLineOfConsole(consoleOutputBase + "100%\n");
            processedCommitCount++;
        }

        executorService.shutdown();

        csvPrintWriter.close();
        lastCommitPrintWriter.close();
    }

    //endregion


    //region helper methods

    /**
     * This method filters modified java files from fileJsonArray.
     *
     * @param fileJsonArray JsonArray of files of a commit
     * @return filtered JsonArray of files which are modified Java files
     */
    private static JsonArray getModifiedJavaFiles(JsonArray fileJsonArray) {
        JsonArray filteredJsonArray = GithubRestApiRepoFileHelper.getFilesByStatus(fileJsonArray, GithubRestApiRepoFileHelper.FileStatus.MODIFIED);
        return GithubRestApiRepoFileHelper.getFilesByExtension(filteredJsonArray, ".java");
    }

    /**
     * This method processes the two file versions of a same file name
     * from two commits (current commit and previous commit).
     *
     * @param fileName          file name
     * @param currentCommitSha  current commit SHA value
     * @param previousCommitSha previous commit SHA value
     * @return output data consisting of file name, method signature changes and current commit SHA
     * @throws Exception if any error occurs
     */
    private OutputDataForEachFileName processEachFileName(final String fileName, final String currentCommitSha, final String previousCommitSha) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<String> currentJavaFileTextThread = executorService.submit(() -> getFileText(currentCommitSha, fileName));
        Future<String> previousJavaFileTextThread = executorService.submit(() -> getFileText(previousCommitSha, fileName));

        String currentJavaFileText = currentJavaFileTextThread.get();
        String previousJavaFileText = previousJavaFileTextThread.get();

        executorService.shutdown();

        List<MethodChange> methodChanges = compareJavaFileMethodParameter(currentJavaFileText, previousJavaFileText);
        return new OutputDataForEachFileName(fileName, methodChanges, currentCommitSha);
    }

    private String getFileText(String commitSha, String fileName) throws Exception {
        return repoFileHelper.getRepoFileAsString(commitSha, fileName);
    }

    /**
     * This method compares method signatures of the two Java files to detect increases of method parameters.
     *
     * @param currentJavaFile  source code of current Java file
     * @param previousJavaFile source code of previous Java file
     * @return a list of method signatures where the current Java file adds parameters of a same method of the previous Java file
     */
    private static List<MethodChange> compareJavaFileMethodParameter(String currentJavaFile, String previousJavaFile) {
        List<MethodChange> changes = new ArrayList<>();

        List<MethodStruct> currentMethodStructs;
        List<MethodStruct> previousMethodStructs;

        try {
            currentMethodStructs = JavaMethodVisitor.getMethodSignatures(new StringReader(currentJavaFile));
            previousMethodStructs = JavaMethodVisitor.getMethodSignatures(new StringReader(previousJavaFile));
        } catch (ParseProblemException e) {
            e.printStackTrace();
            return changes;
        }

        MultiValuedMap<String, MethodStruct> previousMethodStructMap = getMethodStructMap(previousMethodStructs);

        for (MethodStruct methodStruct : currentMethodStructs) {
            String methodStructKey = getMethodStructKey(methodStruct);

            Collection<MethodStruct> foundMethodStructs = previousMethodStructMap.get(methodStructKey);

            if (foundMethodStructs.isEmpty()) continue;  // added method, so ignore
            if (foundMethodStructs.contains(methodStruct)) continue;  // same method found, so ignore

            MethodStruct foundMethodStruct = foundMethodStructs.iterator().next();

            if (foundMethodStruct.parameters.size() < methodStruct.parameters.size()) {  // parameter added, so we have found our required condition
                changes.add(new MethodChange(foundMethodStruct, methodStruct));
            }
        }

        return changes;
    }

    /**
     * This method generates a map of method structures to identify a same method structure with different arguments.
     * The key part of the map consists of the string part without any argument.
     * The value part of the map consists of the method structure itself.
     *
     * @param methodStructs the list of method structures
     * @return a map of method structures
     */
    private static MultiValuedMap<String, MethodStruct> getMethodStructMap(List<MethodStruct> methodStructs) {
        MultiValuedMap<String, MethodStruct> methodStructMap = new ArrayListValuedHashMap<>(methodStructs.size());

        for (MethodStruct methodStruct : methodStructs) {
            methodStructMap.put(getMethodStructKey(methodStruct), methodStruct);
        }

        return methodStructMap;
    }

    /**
     * This method obtains the string representation of method structure without any argument.
     *
     * @param methodStruct method structure
     * @return the string representation of method structure without any argument
     */
    private static String getMethodStructKey(MethodStruct methodStruct) {
        String methodStructString = methodStruct.toString();
        return methodStructString.substring(0, methodStructString.lastIndexOf(':') + 1);
    }

    private void writeToFile(PrintWriter csvPrintWriter, OutputDataForEachFileName outputDataForEachFileName) {
        for (MethodChange methodChange : outputDataForEachFileName.methodChanges) {
            String oldMethodStructString = methodChange.oldMethodStruct.toString();
            String newMethodStructString = methodChange.newMethodStruct.toString();

            csvPrintWriter.printf(
                    "%s,\"%s\",\"%s\",\"%s\"\n",
                    outputDataForEachFileName.currentCommitSha,
                    outputDataForEachFileName.fileName,
                    oldMethodStructString.substring(oldMethodStructString.indexOf(':') + 1),
                    newMethodStructString.substring(newMethodStructString.indexOf(':') + 1));
        }
    }

    /**
     * This method is suitable for writing any single line status file.
     *
     * @param line        line to write
     * @param printWriter current printWriter
     * @param fileName    fileName in which the line will be written
     * @return new print writer for future writing
     * @throws Exception if any error occurs
     */
    private static PrintWriter writeToFirstLine(String line, PrintWriter printWriter, String fileName) throws Exception {
        printWriter.close();
        PrintWriter newPrintWriter = new PrintWriter(new FileWriter(fileName), true);
        newPrintWriter.println(line);
        return newPrintWriter;
    }

    private static void printAndReplaceAtSameLineOfConsole(String string) {
        System.out.print('\r');
        System.out.print(string);
    }

    //endregion


    //region helper structures

    /**
     * This class is the structure of method signature changes.
     */
    private static class MethodChange {
        public final MethodStruct oldMethodStruct;
        public final MethodStruct newMethodStruct;

        public MethodChange(MethodStruct oldMethodStruct, MethodStruct newMethodStruct) {
            this.oldMethodStruct = oldMethodStruct;
            this.newMethodStruct = newMethodStruct;
        }
    }

    /**
     * This class is the structure of output data.
     */
    private static class OutputDataForEachFileName {
        public final String fileName;
        public final List<MethodChange> methodChanges;
        public final String currentCommitSha;

        public OutputDataForEachFileName(String fileName, List<MethodChange> methodChanges, String currentCommitSha) {
            this.fileName = fileName;
            this.methodChanges = methodChanges;
            this.currentCommitSha = currentCommitSha;
        }
    }

    //endregion
}
