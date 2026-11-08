package org.westwoodrobots.pathrecorder.ivy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.westwoodrobots.pathrecorder.Recording;
import org.westwoodrobots.pathrecorder.route.RouteBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GoldenTest {
    static final Path DIR = Paths.get(System.getProperty("goldenDir"));
    static final boolean UPDATE = Boolean.parseBoolean(System.getProperty("updateGolden", "false"));

    static Stream<String> names() {
        return Scenarios.NAMES.stream();
    }

    static String generate(String name) throws IOException {
        Path json = DIR.resolve(name + ".json");
        if (UPDATE) write(json, Scenarios.trace(name).toJson());
        Recording recording = Recording.fromJson(read(json));
        return new IvyBackend().generate(new RouteBuilder(Scenarios.config(name)).build(recording), Scenarios.config(name));
    }

    @ParameterizedTest
    @MethodSource("names")
    void matchesGolden(String name) throws IOException {
        String actual = generate(name);
        Path expected = DIR.resolve(name + ".java.txt");
        if (UPDATE) write(expected, actual);
        assertEquals(read(expected), actual);
    }

    @ParameterizedTest
    @MethodSource("names")
    void isDeterministic(String name) throws IOException {
        assertEquals(generate(name), generate(name));
    }

    static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private static void write(Path p, String s) throws IOException {
        Files.createDirectories(p.getParent());
        Files.write(p, s.getBytes(StandardCharsets.UTF_8));
    }
}
