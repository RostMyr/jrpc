package com.github.rostmyr.jrpc.fibers;

import com.github.rostmyr.jrpc.fibers.bytecode.FiberTransformerResult;
import com.github.rostmyr.jrpc.fibers.bytecode.FiberTransformer;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static java.nio.file.Files.isRegularFile;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rostyslav Myroshnychenko
 * on 01.06.2018.
 */
public class TestFiber {

    @Test
    public void manualInstrumentationTest() throws IOException {
        FiberTransformer instrumentation = new FiberTransformer(TestFiberModel.class, false);
        FiberTransformerResult result = instrumentation.instrument();
        Files.write(Paths.get(TestFiberModel.class.getSimpleName() + ".class"), result.getMainClass());
        for (Map.Entry<String, byte[]> fiber : result.getFibers().entrySet()) {
            Files.write(Paths.get(fiber.getKey() + ".class"), fiber.getValue());
        }
    }

    @Test
    public void shouldInstrumentClasses() throws IOException {
        // GIVEN
        Path basePath = Paths.get("src/test/resources/");
        Path mainClass = basePath.resolve("TestFiberModel.class");

        Map<String, byte[]> innerClassesByNames = Files.walk(basePath.resolve("expected"))
            .filter(path -> isRegularFile(path))
            .collect(toMap(path -> path.getFileName().toString(), TestFiber::readAllBytes));

        // WHEN
        FiberTransformerResult result = new FiberTransformer(readAllBytes(mainClass), false).instrument();

        // THEN
        assertThat(result.getMainClass()).isEqualTo(innerClassesByNames.get(mainClass.getFileName().toString()));
        for (Map.Entry<String, byte[]> innerClassByName : result.getFibers().entrySet()) {
            assertThat(innerClassesByNames.get(innerClassByName.getKey() + ".class")).isEqualTo(innerClassByName.getValue());
        }
    }

    private static byte[] readAllBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException("Error during reading a filed", e);
        }
    }
}
