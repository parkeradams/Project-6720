package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CompilationUnitHelper {

    public static CompilationUnit CreateCompilationUnit(CompilationUnit cu, String fullClassPath) {


        FileInputStream in = null;
        String fullPath = fullClassPath;
        JavaParser jp = new JavaParser();
        try {
            String current = new java.io.File(".").getCanonicalPath();
//            Debugger.log("Current dir:" + current);
        } catch (Exception ex2) {

        }

        try {
            in = new FileInputStream(fullPath);
        } catch (FileNotFoundException fex) {
//            Debugger.log("file not found");

        }

        try {
            // parse the file
            cu = StaticJavaParser.parse(in);

        } catch (Exception pex) {
            pex.printStackTrace();

        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return cu;
    }

    public static CompilationUnit CreateCompilationUnit(String fullClassPath) {
        FileInputStream in = null;
        String fullPath = fullClassPath;
        CompilationUnit cu = null;
        try {
            String current = new java.io.File(".").getCanonicalPath();
//            Debugger.log("Current dir:" + current);
        } catch (Exception ex2) {
            return null;

        }

        try {
            in = new FileInputStream(fullPath);
        } catch (FileNotFoundException fex) {
//            Debugger.log("file not found");
            return null;

        }

        try {
            // parse the file
            cu = StaticJavaParser.parse(in);

        } catch (Exception ex) {
//            Debugger.log(ex);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return cu;
    }
}
