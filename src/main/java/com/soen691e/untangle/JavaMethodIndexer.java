package com.soen691e.untangle;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.*;

/**
 * Produces an index of the methods and the lines they cover in a java project.
 */
public class JavaMethodIndexer {

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addOption("project_root", true, "The path to the source root.");

        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);

        if (!cmd.hasOption("project_root")) {
            HelpFormatter helper = new HelpFormatter();
            helper.printHelp("java_method_indexer", options);
            System.exit(1);
        }

        String root = cmd.getOptionValue("project_root");

        PathMatcher javaMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
        JsonFactory jsonFactory = new JsonFactory();
        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(writer);
        jsonGenerator.writeStartObject();

        Files.walk(Paths.get(root))
                .filter(javaMatcher::matches)
                .map(Path::toFile)
                .forEach(file -> {
                    try {
                        jsonGenerator.writeFieldName(file.getName());
                        jsonGenerator.writeStartArray();
                        CompilationUnit compilationUnit = JavaParser.parse(file);

                        compilationUnit.getNodesByType(MethodDeclaration.class)
                                .forEach(methodDeclaration -> {
                                    try {
                                        jsonGenerator.writeStartObject();
                                        jsonGenerator.writeStringField("methodName", methodDeclaration.getName().getIdentifier());
                                        methodDeclaration.getBegin().ifPresent(x -> {
                                            try {
                                                jsonGenerator.writeNumberField("startLine", x.line);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        methodDeclaration.getEnd().ifPresent(x -> {
                                            try {
                                                jsonGenerator.writeNumberField("endLine", x.line);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        });

                                        jsonGenerator.writeEndObject();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                        jsonGenerator.writeEndArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        jsonGenerator.writeEndObject();
        jsonGenerator.close();
        System.out.println(writer);
        writer.close();
    }
}
