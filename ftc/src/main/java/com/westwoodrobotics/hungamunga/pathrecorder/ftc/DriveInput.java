package com.westwoodrobotics.hungamunga.pathrecorder.ftc;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.hardware.Gamepad;

public interface DriveInput {
    DrivePowers powers(Gamepad gamepad, Pose pose);

    // same stick mapping as the pedro quickstart localization test
    static DriveInput robotCentric() {
        return (g, pose) -> new DrivePowers(-g.left_stick_y, -g.left_stick_x, -g.right_stick_x);
    }

    static DriveInput fieldCentric(double offsetHeading) {
        DriveInput robot = robotCentric();
        return (g, pose) -> ManualDrive.fieldCentric(robot.powers(g, pose), pose.heading(), offsetHeading);
    }
}
