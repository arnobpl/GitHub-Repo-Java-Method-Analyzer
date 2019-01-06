import githubRepo.GithubRepoJavaFileAnalyzer;
import org.apache.commons.collections4.iterators.PeekingIterator;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * This is the main class which will start the program in console mode.
 */
public class ConsoleStarter {
    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                processWithArguments(args);
            } catch (NoSuchElementException e) {
                System.err.println("Invalid arguments have been passed.");
                e.printStackTrace();
            }
            return;
        }

        System.out.println("Welcome to GithubRepoJavaMethodAnalyzer.");
        System.out.println("This program analyzes the increases of the parameters of the method declarations" +
                " of Java source files in the commits of the master branch of any GitHub repository.");

        System.out.println();
        System.out.println("Usage arguments: repo_full_name [-u username password]" +
                " [-p processed_commit_limit] [-t thread_count]");
        System.out.println();

        System.out.println("The output CSV file and a state file (which includes the last processed commit's SHA value)" +
                " will be created in the current directory. Both of the file names will contain repo_full_name as prefix.");
    }

    private static void processWithArguments(String[] args) throws NoSuchElementException {
        PeekingIterator<String> argIterator = new PeekingIterator<>(Arrays.asList(args).iterator());

        // repo_full_name
        final String repoFullName = argIterator.next();

        // [-u username password]
        String username = "";
        String password = "";
        if (argIterator.hasNext() && "-u".equals(argIterator.peek().toLowerCase())) {
            argIterator.next();
            username = argIterator.next();
            password = argIterator.next();
        }

        // [-p processed_commit_limit]
        long processedCommitLimit = 0L;
        if (argIterator.hasNext() && "-p".equals(argIterator.peek().toLowerCase())) {
            argIterator.next();
            try {
                processedCommitLimit = Long.parseLong(argIterator.next());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // [-t thread_count]
        int threadCount = 0;
        if (argIterator.hasNext() && "-t".equals(argIterator.peek().toLowerCase())) {
            argIterator.next();
            try {
                threadCount = Integer.parseInt(argIterator.next());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        try {
            GithubRepoJavaFileAnalyzer githubRepoJavaFileAnalyzer = new GithubRepoJavaFileAnalyzer(repoFullName, username, password);

            if (threadCount != 0)
                githubRepoJavaFileAnalyzer.setThreadCount(threadCount);

            githubRepoJavaFileAnalyzer.analyzeJavaFileMethodParameterInMasterBranchCommits(processedCommitLimit);
            System.out.println("Success!");
        } catch (Exception e) {
            e.printStackTrace();

            try {
                System.err.println("Error occurred! Press ENTER to exit...");
                //noinspection ResultOfMethodCallIgnored
                System.in.read();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
