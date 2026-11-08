package org.westwoodrobots.pathrecorder.ivy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.algorithm.Algorithm;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Localizer;
import org.westwoodrobots.pathrecorder.OutputStyle;
import org.westwoodrobots.pathrecorder.RecorderConfig;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CompileTest {
    private static final String PKG = "org.firstinspires.ftc.teamcode";
    private static final String[] STUBS = {"Intake:collect", "Lift:score", "Claw:open"};

    @TempDir
    Path dir;

    static Stream<String> names() {
        return Scenarios.NAMES.stream();
    }

    @ParameterizedTest
    @MethodSource("names")
    void generatedCodeCompilesAndBuildsPaths(String name) throws Exception {
        RecorderConfig config = Scenarios.config(name);
        String code = GoldenTest.generate(name);
        String className = config.className;
        if (config.style == OutputStyle.SNIPPET) {
            className = "SnippetHost";
            code = wrapSnippet(code, config);
        }
        Path src = dir.resolve("src");
        Files.createDirectories(src);
        List<File> files = new ArrayList<>();
        files.add(write(src.resolve(className + ".java"), code));
        for (String stub : STUBS) {
            String[] s = stub.split(":");
            files.add(write(src.resolve(s[0] + ".java"), "package " + PKG + ";\n\nimport com.pedropathing.ivy.Command;\n\n"
                    + "public class " + s[0] + " {\n    public Command " + s[1] + "() {\n        return Command.NOOP;\n    }\n}\n"));
        }
        Path out = dir.resolve("out");
        Files.createDirectories(out);
        String classpath = System.getProperty("java.class.path") + File.pathSeparator + System.getProperty("ivyPedroJar");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StringWriter log = new StringWriter();
        List<String> args = new ArrayList<>(Arrays.asList("--release", "8", "-Xlint:-options", "-classpath", classpath, "-d", out.toString()));
        boolean ok = compiler.getTask(log, null, null, args, null, compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)
                .getJavaFileObjectsFromFiles(files)).call();
        assertTrue(ok, log + "\n" + code);

        URL[] urls = {out.toUri().toURL(), new File(System.getProperty("ivyPedroJar")).toURI().toURL()};
        try (URLClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader())) {
            Class<?> host = loader.loadClass(PKG + "." + className);
            Method create = Arrays.stream(host.getMethods()).filter(m -> m.getName().equals("create")).findFirst().get();
            Object[] params = new Object[create.getParameterCount()];
            params[0] = new Follower(stub(Localizer.class), stub(Drivetrain.class), stub(Algorithm.class));
            for (int i = 1; i < params.length; i++) params[i] = create.getParameterTypes()[i].getConstructor().newInstance();
            assertNotNull(create.invoke(null, params));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> null);
    }

    private static String wrapSnippet(String snippet, RecorderConfig config) {
        int split = snippet.indexOf("\n\n");
        String imports = snippet.substring(0, split);
        String body = snippet.substring(split + 2);
        StringBuilder b = new StringBuilder("package " + PKG + ";\n\n" + imports + "\nimport com.pedropathing.follower.Follower;\n\n");
        b.append("public final class SnippetHost {\n    public static Command create(Follower ").append(config.followerName)
                .append(", Intake intake, Lift lift, Claw claw) {\n");
        for (String line : body.split("\n")) b.append("        ").append(line).append('\n');
        b.append("        return ").append(config.commandName).append(";\n    }\n}\n");
        return b.toString();
    }

    private static File write(Path p, String s) throws IOException {
        Files.write(p, s.getBytes(StandardCharsets.UTF_8));
        return p.toFile();
    }
}
