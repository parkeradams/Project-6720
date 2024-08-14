package org.example;


import com.github.javaparser.ast.CompilationUnit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

import static org.example.CompilationUnitHelper.CreateCompilationUnit;


// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    /**
     * This method runs the test case provided by the user at the location provided
     * @param projectFilepath
     * @param testCommand
     * @return
     */
    public static boolean RunTest(String projectFilepath, String testCommand){
        String command = "(cd "+ projectFilepath + " ; " + testCommand + ") > out.txt";
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("bash", "-c", command);

        try {
            Process p = pb.start();
            int exitVal = p.waitFor();
//            TimeUnit.SECONDS.sleep(5);


        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        //read through the output file and see if we have a fail or a pass or a compilation error

        Path outputFile = Path.of("out.txt");
        try {
            String testOutput = Files.readString(outputFile);

            //if we failed the test, return a true. if not return false.
            return testOutput.contains("Tests run: 1, Failures: 1, Errors: 0, Skipped: 0");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * Splits the input set s into n subsets
     * https://github.com/benjholla/ddmin/blob/master/DeltaDebug/src/dd/DeltaDebug.java
     * @param s
     * @param n
     * @return
     */
    private static <E> List<List<E>> split(List<E> s, int n) {
        List<List<E>> subsets = new LinkedList<List<E>>();
        int position = 0;
        for (int i = 0; i < n; i++) {
            List<E> subset = s.subList(position, position + (s.size() - position) / (n - i));
            subsets.add(subset);
            position += subset.size();
        }
        return subsets;
    }

    /**
     * Returns the difference of set a and set b
     * // code inspired from https://github.com/benjholla/ddmin/blob/master/DeltaDebug/src/dd/DeltaDebug.java
     * @param a
     * @param b
     * @return
     */
    private static <E> List<E> difference(List<E> a, List<E> b) {
        List<E> result = new LinkedList<E>();
        result.addAll(a);
        result.removeAll(b);
        return result;
    }

    /**
     * This test modifies the test case file at the path given based on the Compilation unit parameter
     * @param testFilepath
     * @param cu
     */
    private static void RevertTestCase(String testFilepath, CompilationUnit cu) {
        try {
            Path filename = Path.of(testFilepath);
            Files.writeString(filename, cu.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method takes an original compilation unit and generates a modified one with the specified list removed.
     * @param ogCu
     * @param subDelta
     * @param testClass
     * @param testName
     * @return
     */
    private static CompilationUnit generateNewCu(CompilationUnit ogCu, List<Integer> subDelta, String testClass, String testName) {
        CompilationUnit newCu = ogCu.clone();
        MethodDeclaration methodDeclaration = newCu.getClassByName(testClass).get().getMethodsByName(testName).get(0);
        BlockStmt testCase = methodDeclaration.getBody().get();

        for(int i = subDelta.size()-1; i >= 0; i--){
            testCase.remove(testCase.getStatements().get(subDelta.get(i)));
        }

        return newCu;

    }
    public static void main(String[] args) {

        //File paths, test command, classname, and test name are all required to run algorithm
        String testFilepath = "/Downloads/commons-validator/src/test/java/org/apache/commons/validator/routines/UrlValidatorTest.java";
        String projectFilepath = "/Downloads/commons-validator";
        String testCommand = "mvn -Dtest=UrlValidatorTest#testValidator276 test";
        String testName = "testValidator276";
        String testClass = "UrlValidatorTest";



        //Compilation unit and backup to revert the file after.
        CompilationUnit cu = CreateCompilationUnit(testFilepath);
        CompilationUnit cuBackup = CreateCompilationUnit(testFilepath);



        //isolate the test case in the class and by its method name
        MethodDeclaration methodDeclaration = cu.getClassByName(testClass).get().getMethodsByName(testName).get(0);

        //this is the number of statements in the test case. This is our delta
        int statementCount = methodDeclaration.getBody().get().getStatements().size();

        //our delta is statements in the test
        //create a list of integers representing lines of code.
        List<Integer> delta = new ArrayList<Integer>();

        for(int i = 0; i < statementCount; i++){
            delta.add(i);
        }

        //backup list for final output
        List<Integer> deltaBackup = new ArrayList<Integer>(delta);


        //ddmin algorithm
        long startTime = System.nanoTime();
        // start with n = 2
        int n = 2;
        System.out.println("DDMIN Begin");
        int counter = 0;
        while (true){
            counter += 1;
            System.out.println("Iteration " + counter);
            // divide the delta into n equal parts t and find the compliments
            List<List<Integer>> subDeltas = split(delta, n);
            System.out.println("n = " + n);

            // Flag for adjusting granularity or delta list
            boolean debugSuccess = false;


            for(List<Integer> subDelta : subDeltas){
                // take the difference of the subdelta as the complement
                List<Integer> complement = difference(delta, subDelta);
                //output to the user
                System.out.println("SubDelta = " + subDelta);
                System.out.println("complement = " + complement);

                //generate a new Compilation unit, as a copy of the original to edit the file
                CompilationUnit newCu = generateNewCu(cu, subDelta, testClass, testName);

                //edit the file to the newCu
                RevertTestCase(testFilepath, newCu);

                //run the test case, removing all the lines from the sub delta
                System.out.println("Running Unit test on the sub delta...");
                if(RunTest(projectFilepath, testCommand)){
                    System.out.println("Test continued to fail, decreasing delta to " + complement);

                    delta = complement;
                    n = 2;
                    System.out.println("new value for n = " + n);
                    debugSuccess = true;
                    break;
                }


                //revert the file to the ogCu
                RevertTestCase(testFilepath, cuBackup);

                //generate the complements compilation unit to remove the test cases.
                CompilationUnit newCuComplement = generateNewCu(cu, complement, testClass, testName);
                //edit the file to the complement
                RevertTestCase(testFilepath, newCuComplement);


                // Run the test case, removing all the lines from the complement delta
                System.out.println("Running Unit test on the complement delta...");
                if(RunTest(projectFilepath, testCommand)){
                    System.out.println("Test continued to fail with complement, decreasing delta to " + subDelta);
                    delta = subDelta;
                    n = Math.max(n - 1, 2);
                    System.out.println("new value for n = " + n);
                    debugSuccess = true;

                    break;
                }
                //revert the file to its original state
                RevertTestCase(testFilepath, cuBackup);


            }

            //if n hasn't been adjusted, aka if there were no successes
            if(!debugSuccess || delta.size() < 2){

                if(n >= delta.size()){
                    System.out.println("Minimum Test case has been found.");
                    System.out.println();
                    //the resulting output is our reduced test case.
                    System.out.println("The test case will reproduce the error with only the following lines: " + delta);

                    // write the final test case method to an output file called FinalTestCase.txt
                    try {
                        for (int value : delta){
                            deltaBackup.remove(value);
                        }

                        CompilationUnit finalTestCaseCu = generateNewCu(cu, deltaBackup, testClass, testName);
                        FileWriter writer = new FileWriter("FinalTestCase.txt");
                        writer.write(finalTestCaseCu.getClassByName(testClass).get().getMethodsByName(testName).get(0).toString());
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    break;
                }
                //increase n
                n = Math.min(n * 2, delta.size());
                System.out.println("new value for n = " + n);

            }



        }

        long endTime = System.nanoTime();
        System.out.println("Total Execution Time: " + TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) + " seconds");
        RevertTestCase(testFilepath, cuBackup);
        System.out.println("Program Complete");

    }

}