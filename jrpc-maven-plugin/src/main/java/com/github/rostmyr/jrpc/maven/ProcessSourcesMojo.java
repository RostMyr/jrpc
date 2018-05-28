package com.github.rostmyr.jrpc.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.github.rostmyr.jrpc.common.bytecode.ClassTransformer;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.file.Files.isRegularFile;

/**
 * Rostyslav Myroshnychenko
 * on 25.05.2018.
 */
@Mojo(
    name = "process-source-files",
    threadSafe = true,
    defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    requiresDependencyResolution = ResolutionScope.TEST
)
public class ProcessSourcesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Set this to "true" to skip plugin execution.
     */
    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() {
        if (skip) {
            getLog().info("Skip processing.");
            return;
        }

        String outputDirectory = project.getBuild().getOutputDirectory();
        getLog().info("Output directory: " + outputDirectory);

        try {
            Stream<Path> walker = getFileTreeWalker(outputDirectory);
            walker.filter(path -> isRegularFile(path))
                .forEach(processCompiledClasses());

        } catch (IOException e) {
            getLog().error("Error during processing the files", e);
        }
    }

    private Consumer<Path> processCompiledClasses() {
        return path -> {
            try {
                byte[] clazz = Files.readAllBytes(path);
                byte[] transformed = new ClassTransformer(clazz).transform();
                if (transformed != clazz) {
                    Files.write(path, transformed, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                }
            } catch (IOException e) {
                getLog().error("Error during reading the file: " + path, e);
            }
        };
    }

    private Stream<Path> getFileTreeWalker(String outputDirectory) throws IOException {
        return Files.walk(Paths.get(outputDirectory), Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS);
    }
}
