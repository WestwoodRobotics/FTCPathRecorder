package com.westwoodrobotics.hungamunga.pathrecorder.ftc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.westwoodrobotics.hungamunga.pathrecorder.CodeGenBackend;
import com.westwoodrobotics.hungamunga.pathrecorder.CommandRuntime;
import com.westwoodrobotics.hungamunga.pathrecorder.RecorderConfig;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class PathRecorderTest {
    @TempDir
    Path dir;

    private final Follower follower = mock(Follower.class);
    private final Gamepad gamepad = mock(Gamepad.class);
    private final Telemetry telemetry = mock(Telemetry.class);
    private final FakeRuntime runtime = new FakeRuntime();
    private final RecorderConfig config = new RecorderConfig();
    private Pose pose = new Pose(8, 72, 0);
    private Velocity velocity = new Velocity(0, 0, 0);
    private long nanos;
    private boolean button;

    @BeforeEach
    void setUp() {
        config.outputDirectory = dir.toString();
        when(follower.pose()).thenAnswer(i -> pose);
        when(follower.velocity()).thenAnswer(i -> velocity);
    }

    private PathRecorder<String> recorder() {
        return new PathRecorder<>(follower, new Pose(8, 72, 0), config, runtime).clocks(() -> nanos, () -> 0L);
    }

    private void loop(PathRecorder<String> rec, double ms) {
        rec.update(gamepad, telemetry);
        nanos += (long) (ms * 1e6);
    }

    @Test
    void setsStartPose() {
        recorder();
        verify(follower).setPose(any(Pose.class));
    }

    @Test
    void heldButtonFiresOnce() {
        AtomicInteger made = new AtomicInteger();
        PathRecorder<String> rec = recorder().action("intake.collect()", () -> button, () -> "cmd" + made.incrementAndGet());
        loop(rec, 10);
        button = true;
        for (int i = 0; i < 5; i++) loop(rec, 10);
        button = false;
        loop(rec, 10);
        assertEquals(1, made.get());
        assertEquals(1, runtime.scheduled.size());
        assertEquals(1, rec.recording().actions.size());
        assertEquals("intake.collect()", rec.recording().actions.get(0).expression);
        assertEquals(0.01, rec.recording().actions.get(0).t, 1e-9);
    }

    @Test
    void eachPressSchedulesAFreshCommand() {
        AtomicInteger made = new AtomicInteger();
        PathRecorder<String> rec = recorder().action("lift.score()", () -> button, () -> "cmd" + made.incrementAndGet());
        for (int i = 0; i < 6; i++) {
            button = i % 2 == 1;
            loop(rec, 10);
        }
        assertEquals(3, runtime.scheduled.size());
        assertEquals("cmd1", runtime.scheduled.get(0));
        assertEquals("cmd3", runtime.scheduled.get(2));
    }

    @Test
    void runtimeExecutesEveryLoop() {
        PathRecorder<String> rec = recorder();
        for (int i = 0; i < 4; i++) loop(rec, 10);
        assertEquals(4, runtime.executions);
    }

    @Test
    void drivesWithQuickstartStickMapping() {
        PathRecorder<String> rec = recorder();
        gamepad.left_stick_y = -1f;
        gamepad.left_stick_x = 0.5f;
        gamepad.right_stick_x = 0.25f;
        loop(rec, 10);
        ArgumentCaptor<DrivePowers> powers = ArgumentCaptor.forClass(DrivePowers.class);
        verify(follower).manual(powers.capture());
        assertEquals(1.0, powers.getValue().forward(), 1e-9);
        assertEquals(-0.5, powers.getValue().strafe(), 1e-9);
        assertEquals(-0.25, powers.getValue().turn(), 1e-9);
        verify(follower).update();
    }

    @Test
    void sampleIntervalLimitsRate() {
        config.sampleInterval = 20;
        PathRecorder<String> rec = recorder();
        for (int i = 0; i < 10; i++) loop(rec, 10);
        assertEquals(5, rec.samples().size());
    }

    @Test
    void previewWaitsForMinimumStopTime() {
        PathRecorder<String> rec = recorder();
        velocity = new Velocity(30, 0, 0);
        for (int i = 0; i < 50; i++) {
            pose = new Pose(8 + i * 0.3, 72, 0);
            loop(rec, 10);
        }
        velocity = new Velocity(0, 0, 0);
        for (int i = 0; i < 14; i++) loop(rec, 10);
        assertNull(rec.route());
        loop(rec, 10);
        loop(rec, 10);
        assertNotNull(rec.route());
        assertEquals(1, rec.route().segmentCount());
    }

    @Test
    void finishExportsAndStopsRecording() {
        PathRecorder<String> rec = recorder().finish(() -> button);
        velocity = new Velocity(30, 0, 0);
        for (int i = 0; i < 50; i++) {
            pose = new Pose(8 + i * 0.6, 72, 0);
            loop(rec, 10);
        }
        velocity = new Velocity(0, 0, 0);
        for (int i = 0; i < 20; i++) loop(rec, 10);
        button = true;
        loop(rec, 10);
        assertTrue(rec.finished());
        verify(follower).hold(any(Pose.class));
        File file = rec.file();
        assertNotNull(file);
        assertTrue(file.getName().endsWith(".java"));
        assertTrue(new File(file.getParentFile(), file.getName().replace(".java", ".json")).isFile());
        assertEquals("code 1", rec.output());
        int samples = rec.samples().size();
        loop(rec, 10);
        assertEquals(samples, rec.samples().size());
        verify(telemetry, atLeastOnce()).addData("segments", 1);
        verify(telemetry, atLeastOnce()).addData("file", file.getName());
    }

    @Test
    void waypointRecordsTime() {
        PathRecorder<String> rec = recorder().waypoint(() -> button);
        loop(rec, 10);
        button = true;
        loop(rec, 10);
        assertEquals(1, rec.recording().waypoints.size());
        assertEquals(0.01, rec.recording().waypoints.get(0), 1e-9);
    }

    @Test
    void noFinishWithoutTrigger() {
        PathRecorder<String> rec = recorder().finish(() -> false);
        for (int i = 0; i < 3; i++) loop(rec, 10);
        assertFalse(rec.finished());
        verify(follower, never()).hold(any(Pose.class));
    }

    @Test
    void pageShowsNewestFileEscaped() throws Exception {
        java.nio.file.Files.write(dir.resolve("20260101-000000.java"), "old".getBytes());
        java.nio.file.Files.write(dir.resolve("20260102-000000.java"), "a < b && c".getBytes());
        String html = RecorderPage.html(dir.toString());
        assertTrue(html.contains("20260102-000000.java"));
        assertTrue(html.contains("a &lt; b &amp;&amp; c"));
        assertTrue(html.contains("document.execCommand('copy')"));
        assertTrue(RecorderPage.html(dir.resolve("missing").toString()).contains("no recording yet"));
    }

    private static final class FakeRuntime implements CommandRuntime<String> {
        final List<String> scheduled = new ArrayList<>();
        int executions;

        @Override
        public void schedule(String command) {
            scheduled.add(command);
        }

        @Override
        public void execute() {
            executions++;
        }

        @Override
        public CodeGenBackend backend() {
            return (route, config) -> "code " + route.segmentCount();
        }
    }
}
