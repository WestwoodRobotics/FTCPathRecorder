package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.westwoodrobots.pathrecorder.RecorderConfig;
import org.westwoodrobots.pathrecorder.ftc.PathRecorder;
import org.westwoodrobots.pathrecorder.ivy.IvyRuntime;
import org.westwoodrobots.pathrecorder.panels.PanelsOverlay;
import org.firstinspires.ftc.teamcode.pedro.Constants;

@TeleOp(name = "Record Auto", group = "PathRecorder")
public class RecordAuto extends OpMode {
    private PathRecorder<Command> recorder;

    @Override
    public void init() {
        Scheduler.reset();
        Follower follower = Constants.create(hardwareMap);
        Claw claw = new Claw(hardwareMap);

        RecorderConfig config = new RecorderConfig();
        config.positionTolerance = 1.0;

        recorder = new PathRecorder<>(follower, new Pose(8, 72, 0), config, IvyRuntime.owned())
                .action("claw.open()", () -> gamepad1.aWasPressed(), claw::open)
                .action("claw.close()", () -> gamepad1.bWasPressed(), claw::close)
                .waypoint(() -> gamepad1.xWasPressed())
                .finish(() -> gamepad1.backWasPressed())
                .overlay(new PanelsOverlay());
    }

    @Override
    public void loop() {
        recorder.update(gamepad1, telemetry);
        telemetry.update();
    }

    @Override
    public void stop() {
        recorder.finish();
    }
}
