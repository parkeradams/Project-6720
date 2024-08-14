package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException {
        // Path to the jacoco.exec file from the Validator program
        Process process = getProcess();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(output);
    }

    private static Process getProcess() throws IOException {
        File workingDirectory = new File("Path-to-folder/Commons-validator");
        ProcessBuilder processBuilder = new ProcessBuilder(
                "Path-to-folder/apache-maven-3.9.8-bin/apache-maven-3.9.8/bin/mvn.cmd",
                "-Dtest=UrlValidatorTest", "test" // First run with test then run with jacoco:report --> // test //jacoco:report
        );
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(workingDirectory);

        return processBuilder.start();
    }
}
