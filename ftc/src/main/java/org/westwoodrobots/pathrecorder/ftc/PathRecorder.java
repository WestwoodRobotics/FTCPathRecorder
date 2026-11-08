package org.westwoodrobots.pathrecorder.ftc;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.westwoodrobots.pathrecorder.ActionEvent;
import org.westwoodrobots.pathrecorder.CommandRuntime;
import org.westwoodrobots.pathrecorder.RecorderConfig;
import org.westwoodrobots.pathrecorder.Recording;
import org.westwoodrobots.pathrecorder.Sample;
import org.westwoodrobots.pathrecorder.route.RouteBuilder;
import org.westwoodrobots.pathrecorder.route.RouteIR;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.firstinspires.ftc.robotcore.external.Telemetry;

public final class PathRecorder<C> {
    private final Follower follower;
    private final Pose start;
    private final RecorderConfig config;
    private final CommandRuntime<C> runtime;
    private final List<Action<C>> actions = new ArrayList<>();
    private final List<Trigger> waypointTriggers = new ArrayList<>();
    private final List<Trigger> finishTriggers = new ArrayList<>();
    private final List<Sample> samples = new ArrayList<>();
    private final List<ActionEvent> events = new ArrayList<>();
    private final List<Double> waypoints = new ArrayList<>();

    private DriveInput drive = DriveInput.robotCentric();
    private RecorderOverlay overlay;
    private LongSupplier clock = System::nanoTime;
    private LongSupplier wallClock = System::currentTimeMillis;

    private long startNanos = -1;
    private long lastSampleNanos = Long.MIN_VALUE;
    private double stillSince = Double.NaN;
    private boolean moved;
    private boolean finished;
    private RouteIR route;
    private String output;
    private File file;
    private String error;

    public PathRecorder(Follower follower, Pose start, RecorderConfig config, CommandRuntime<C> runtime) {
        this.follower = follower;
        this.start = start;
        this.config = config;
        this.runtime = runtime;
        follower.setPose(start);
    }

    public PathRecorder<C> action(String expression, BooleanSupplier trigger, Supplier<? extends C> command) {
        actions.add(new Action<C>(expression, new Trigger(trigger), command));
        return this;
    }

    public PathRecorder<C> waypoint(BooleanSupplier trigger) {
        waypointTriggers.add(new Trigger(trigger));
        return this;
    }

    public PathRecorder<C> finish(BooleanSupplier trigger) {
        finishTriggers.add(new Trigger(trigger));
        return this;
    }

    public PathRecorder<C> drive(DriveInput input) {
        this.drive = input;
        return this;
    }

    public PathRecorder<C> overlay(RecorderOverlay overlay) {
        this.overlay = overlay;
        return this;
    }

    PathRecorder<C> clocks(LongSupplier nanos, LongSupplier millis) {
        this.clock = nanos;
        this.wallClock = millis;
        return this;
    }

    public void update(Gamepad gamepad, Telemetry telemetry) {
        long now = clock.getAsLong();
        if (startNanos < 0) startNanos = now;
        double t = (now - startNanos) / 1e9;

        if (!finished) follower.manual(drive.powers(gamepad, follower.pose()));
        follower.update();

        if (!finished) {
            record(t, now);
            for (Action<C> a : actions) {
                if (a.trigger.poll()) {
                    events.add(new ActionEvent(t, a.expression));
                    runtime.schedule(a.command.get());
                }
            }
            for (Trigger w : waypointTriggers) {
                if (w.poll()) waypoints.add(t);
            }
            for (Trigger f : finishTriggers) {
                if (f.poll()) {
                    finish();
                    break;
                }
            }
        }
        runtime.execute();
        if (overlay != null) overlay.draw(samples(), route, follower.pose());
        if (telemetry != null) telemetry(telemetry);
    }

    public void finish() {
        if (finished) return;
        finished = true;
        follower.hold(follower.pose());
        route = new RouteBuilder(config).build(recording());
        output = runtime.backend().generate(route, config);
        try {
            file = Exporter.write(config.outputDirectory, wallClock.getAsLong(), output, recording().toJson());
        } catch (IOException | RuntimeException e) {
            error = e.getMessage();
        }
    }

    public Recording recording() {
        return new Recording(start, samples, events, waypoints);
    }

    public List<Sample> samples() {
        return Collections.unmodifiableList(samples);
    }

    public boolean finished() {
        return finished;
    }

    public RouteIR route() {
        return route;
    }

    public String output() {
        return output;
    }

    public File file() {
        return file;
    }

    private void record(double t, long now) {
        Pose pose = follower.pose();
        Velocity v = follower.velocity();
        if (lastSampleNanos == Long.MIN_VALUE || now - lastSampleNanos >= (long) (config.sampleInterval * 1e6)) {
            samples.add(new Sample(t, pose.x(), pose.y(), pose.heading(), v.vx, v.vy, v.omega));
            lastSampleNanos = now;
        }
        boolean still = Math.hypot(v.vx, v.vy) < config.stopSpeed;
        if (!still) {
            moved = true;
            stillSince = Double.NaN;
            return;
        }
        if (Double.isNaN(stillSince)) stillSince = t;
        // refit once per stop, the robot is not moving so a slow loop is safe here
        if (config.livePreview && moved && (t - stillSince) * 1000 >= config.minStopTime) {
            moved = false;
            route = new RouteBuilder(config).build(recording());
        }
    }

    private void telemetry(Telemetry telemetry) {
        if (!finished) {
            Pose p = follower.pose();
            telemetry.addData("recording", "%d samples, %d actions, %d waypoints", samples.size(), events.size(), waypoints.size());
            telemetry.addData("pose", String.format(Locale.US, "x %.1f  y %.1f  h %.1f", p.x(), p.y(), Math.toDegrees(p.heading())));
            return;
        }
        telemetry.addData("segments", route.segmentCount());
        telemetry.addData("curves", route.curveCount());
        telemetry.addData("max fit error", String.format(Locale.US, "%.2f in", route.maxFitError()));
        telemetry.addData("file", file != null ? file.getName() : "not saved: " + error);
    }

    private static final class Action<C> {
        final String expression;
        final Trigger trigger;
        final Supplier<? extends C> command;

        Action(String expression, Trigger trigger, Supplier<? extends C> command) {
            this.expression = expression;
            this.trigger = trigger;
            this.command = command;
        }
    }
}
